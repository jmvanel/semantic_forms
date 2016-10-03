package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import scala.language.postfixOps
import org.w3.banana.RDFPrefix
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.RDFOPerationsDB
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.services.URIManagement
import java.io.FileWriter
import java.io.File
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDFSPrefix
import org.w3.banana.Prefix
import deductions.runtime.utils.RDFPrefixes
import java.util.Date
import java.io.FileOutputStream

/**
 * merge Duplicates among instances of given class URI;
 * use cases: merge FOAF duplicates, etc;
 *  #41 https://github.com/jmvanel/semantic_forms/issues/41
 */
trait DuplicateCleaner[Rdf <: RDF, DATASET]
    extends BlankNodeCleanerBase[Rdf, DATASET]
    with PropertiesCleaner[Rdf, DATASET]
    with PropertyDomainCleaner[Rdf, DATASET]
    with RDFCacheAlgo[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with RDFPrefixes[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with URIManagement {
  import ops._
      import rdfStore.graphStoreSyntax._
      import rdfStore.transactorSyntax._

  /** merge Marker in case of _automatic merge */
  val mergeMarker = " (F)"
  /** merge Marker in case of Specification based merge */
  val mergeMarkerSpec = " (FS)"

  type URIMergeSpecifications = List[URIMergeSpecification]
  case class URIMergeSpecification(replacedURI: Rdf#URI, replacingURI: Rdf#URI,
    newLabel: String = "", comment: String = "")

  /**
   * merges Duplicates automatically among instances of given class URI,
   * based on criterium: instanceLabel() giving identical result;
   * moreover if class URI == owl:ObjectProperty the rdfs:range's must be equal
   * includes transactions
   */
  def removeAllDuplicates(classURI: Rdf#URI, lang: String = ""): String = {

    val instanceLabels2URIsMap: Map[String, Seq[Rdf#URI]] =
      rdfStore.rw(dataset, {
        indexInstanceLabels(classURI, lang)
      }).get

    var duplicatesCount = 0
    var propertiesHavingDuplicates = 0

    val instanceLabels2URIsMap2 = rdfStore.rw(dataset, {
      checkRdfsRanges(instanceLabels2URIsMap, classURI)
    }).get

    for (labelAndURIs <- instanceLabels2URIsMap2) {
      val label = labelAndURIs._1
      println(s"""looking at label "$label" """)
      try {
        val (uriTokeep, duplicateURIs) = tellDuplicates(labelAndURIs._2)
        if (!duplicateURIs.isEmpty) {
          propertiesHavingDuplicates = propertiesHavingDuplicates + 1
          duplicatesCount = duplicatesCount + duplicateURIs.size
          println(s"""Deleting duplicates for "${label}" uriTokeep <$uriTokeep>, delete count ${duplicateURIs.size}""")

          val named_graph = removeDuplicatesFromSeq(uriTokeep, duplicateURIs)

          storeLabelWithMergeMarker(uriTokeep, merge_marker = mergeMarkerSpec,
            graphToWrite = named_graph)

          addRestructuringComment(uriTokeep, duplicateURIs)
        }
      } catch {
        case t: Throwable =>
          println("WARNING: removeAllDuplicates: " + t.getClass + " " + t.getLocalizedMessage)
      }
    }
    s"propertiesHavingDuplicates: $propertiesHavingDuplicates, duplicatesCount: $duplicatesCount"
  }

  /**
   * remove Duplicates From Specifications:
   * after calling [[removeDuplicatesFromSeq]] on given merge Specifications,
   *  replace multiple rdfs:labels with the given new label from mergeSpecifications;
   *  add a merge Marker to rdfs:label's
   */
  def removeDuplicatesFromSpec(uriTokeep: Rdf#URI,
                               mergeSpecifications0: URIMergeSpecifications,
                               auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()): Unit = {
    println(s"uriTokeep <$uriTokeep>")
    for (mergeSpecification <- mergeSpecifications0) {
      val assertion = mergeSpecification.replacedURI != "" &&
        mergeSpecification.replacingURI != ""
      println(s"mergeSpecification $mergeSpecification assertion $assertion")
      System.out.flush() ; if (!assertion) System.exit(0)
    }
    println("XXXXX")

    val mergeSpecifications = mergeSpecifications0.filter { mergeSpecification =>
      mergeSpecification.replacingURI != nullURI &&
        (mergeSpecification.replacedURI != nullURI ||
          mergeSpecification.newLabel != "" ||
          mergeSpecification.comment != "")
    }
    val duplicateURIs: List[Rdf#URI] =
      for (mergeSpecification <- mergeSpecifications) yield mergeSpecification.replacedURI

    val named_graph = removeDuplicatesFromSeq(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI],
      auxiliaryOutput)

    //  create and replace triples for new rdfs:label

    val newLabels = for (
      mergeSpecification <- mergeSpecifications;
      newLabel = mergeSpecification.newLabel;
      if (newLabel != "")
    ) yield mergeSpecification.newLabel
    if (newLabels.size > 1) System.err.println(s"WARING! several new Labels for $uriTokeep: $newLabels")
    val newLabel = newLabels.headOption.getOrElse("")
    storeLabelWithMergeMarker(uriTokeep, merge_marker = mergeMarkerSpec,
      graphToWrite = named_graph, newLabel = newLabel)

    val transaction = rdfStore.rw(dataset, {
      for (mergeSpecification <- mergeSpecifications) {
        // recycle old rdfs:label's as skos:altLabel's
        if (mergeSpecification.replacedURI != mergeSpecification.replacingURI) {
          val labelsFromReplacedURI: List[Quad] = removeFromQuadQuery(mergeSpecification.replacedURI, rdfs.label, ANY)
          if (!labelsFromReplacedURI.isEmpty) {
            val newTriples = for (removedQuad <- labelsFromReplacedURI)
              yield Triple(mergeSpecification.replacingURI, skos("altLabel"), removedQuad._1.objectt);
            rdfStore.appendToGraph(dataset, named_graph, makeGraph(newTriples))
          }
        }

        // add rdfs:comment from given merge Specification
        if (mergeSpecification.comment != "") {
          val commentTriple = Triple(mergeSpecification.replacingURI,
            rdfs.comment, Literal(mergeSpecification.comment))
          rdfStore.appendToGraph(dataset, named_graph, makeGraph(List(commentTriple)))
        }
      }
      addRestructuringCommentNoTr(uriTokeep, duplicateURIs, mergeMarkerSpec)
    })
  }

  /** add a merge Marker to rdfs:label's */
  private def storeLabelWithMergeMarker(replacingURI: Rdf#URI,
                                        newLabel: String = "",
                                        merge_marker: String = mergeMarker,
                                        graphToWrite: Rdf#URI = URI("")) = {
    rdfStore.rw(dataset, {
      if( replacingURI.toString() != "" )
        println(s"DDDDDDDDDDDDDDD replacingURI $replacingURI")
        
      val label = if (newLabel != "") {
        newLabel
      } else {
        implicit val graph = allNamedGraph: Rdf#Graph
        getStringHeadOrElse(replacingURI, rdfs.label)
//        val tryGraph = rdfStore.getGraph(dataset, graphToWrite)
//        tryGraph.map { graph =>
//          val originalLabel = getObjects(graph, replacingURI, rdfs.label).
//            headOption.getOrElse(Literal(""))
//          getStringOrElse(originalLabel)
//        }
      }
      if (label != "") {
        val newLabelTriple = Triple(replacingURI,
          rdfs.label, Literal(label + merge_marker))
        println(s"DDDDDDDDDDDDDDD newLabelTriple $newLabelTriple")
//        rdfStore.appendToGraph(dataset, graphToWrite, makeGraph(List(newLabelTriple)))
       replaceRDFTriple(newLabelTriple, graphToWrite, dataset)
      }
    })
  }
    
  /** add restructuring comment (annotation property),
   *  telling with URI's have been merged; DOES NOT include transaction */
  private def addRestructuringCommentNoTr(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI],
      comment: String=mergeMarker,
      graphToWrite: Rdf#URI=URI("")) = {
    val restrucProp = restruc("restructructionComment")
    val dupsComment = for (duplicateURI <- duplicateURIs) yield fromUri(duplicateURI)
    val restructructionComment = s"""Fusion $comment le ${new Date}
    vers $uriTokeep
    à partir de
    ${dupsComment.mkString(",\n")}
    """
    val restructructionCommentTriple = Triple(
      uriTokeep,
      restrucProp,
      Literal(restructructionComment))
    rdfStore.appendToGraph(dataset, graphToWrite,
      makeGraph(List(restructructionCommentTriple)))
  }

  private def addRestructuringComment(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI],
        comment: String=mergeMarker,
        graphToWrite: Rdf#URI=URI("")) = {    
    val transaction = rdfStore.rw(dataset, {
      addRestructuringCommentNoTr(uriTokeep, duplicateURIs, comment, graphToWrite)
    })
  }

  /**
   * remove Duplicates in given duplicate URI's
   *  the algorithm:
   * - from triples <duplicateURIs> ?P ?O
   *   create triples <uriTokeep> ?P ?O
   * - delete triples <duplicateURIs> ?P ?O
   *
   * - from reverse triples ?S ?P1 <duplicateURIs>
   *   create triples ?S ?P1 <uriTokeep>
   * - delete triples ?S ?P1 <duplicateURIs>
   *
   * includes transactions
   */
  private def removeDuplicatesFromSeq(
    uriTokeep: Rdf#URI,
    duplicateURIs: Seq[Rdf#URI],
    auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()): Rdf#URI = {

    if (uriTokeep != nullURI) {
      copySubjectPropertyPairs(uriTokeep, duplicateURIs)
      val quadsOfDuplicateURIs = copyPropertyValuePairs(uriTokeep, duplicateURIs)
      val named_graph: Rdf#URI = quadsOfDuplicateURIs(0)._2

      if (duplicateURIs.contains(uriTokeep)) {
        println(s"CAUTION: duplicateURIs contains uriTokeep=$uriTokeep")
      }
      removeQuadsWithSubjects(duplicateURIs.toList.diff(List(uriTokeep)))
      removeQuadsWithObjects(duplicateURIs.toList.diff(List(uriTokeep)))
      println(s"Deleted ${duplicateURIs.size} duplicate URI's for <$uriTokeep>")

      processKeepingTrackOfDuplicates(uriTokeep, duplicateURIs, auxiliaryOutput)
      processMultipleRdfsDomains(uriTokeep, duplicateURIs)

      rdfStore.r(dataset,
        println(s"removeDuplicates: named graphs ${listNames().mkString(", ")}"))
      named_graph
    } else
      nullURI
  }

  /** removeQuads With given Subjects; includes transaction */
  protected def removeQuadsWithSubjects(subjects: Seq[Rdf#Node]) = {
    val transaction = rdfStore.rw(dataset, {
      for (dupURI <- subjects) {
        println(s"removeQuadsWithSubject <$dupURI> size() $datasetSizeNoTR")
        removeQuadsWithSubject(dupURI)
        println(s"removeQuadsWithSubject <$dupURI> size() after $datasetSizeNoTR")
      }
    })
  }

  /** removeQuads With given objects; includes transaction */
  protected def removeQuadsWithObjects(objects: Seq[Rdf#Node]) = {
    val transaction = rdfStore.rw(dataset, {
      for (dupURI <- objects) {
        removeQuadsWithObject(dupURI)
      }
    })
  }

  /**
   * tell which URI's are Duplicates;
   * that is, for each given Seq[Rdf#URI], distinguish one URI as the one to keep
   *
   *  NOTE: this is the function you may want to override,
   *  depending on your criteria for duplication, and URI creation policy.
   *
   * The URI considered as non-duplicate will be
   * HTTP URI's not in the DNS domain of the server,
   * URI in the urn: scheme that were loaded in batch from CVS data
   * In the case both are present, the HTTP URI will be kept.
   *
   * On the contrary, The URI considered as duplicate will be
   * HTTP URI's in the DSN domain of the server (hence created by user), when there is a corresponding URI of the forms above.
   *
   * The criterion for duplication should involve not only givenName and familyName.
   * So it is preferable to leverage on function instanceLabel().
   *
   * @return the URI's considered as non-duplicate, and for each a list of the duplicates
   */
  protected def tellDuplicates(uris: Seq[Rdf#URI]): (Rdf#URI, Seq[Rdf#URI]) = {
    if (uris.size <= 1)
      // nothing to do by caller! no Duplicates
      return (ops.URI(""), Seq())

    def filterURIsByScheme(scheme: String) = {
      uris.filter { uri =>
        val jURI = new java.net.URI(uri.toString())
        jURI.getScheme == scheme
      }
    }
    /* @return the first URI matching one of the prefixes. */
    def filterURIsByStartsWith(uris: Seq[Rdf#URI], prefixes: Seq[String]): Option[Rdf#URI] = {
      val candidates = for (
        prefix <- prefixes;
        matching <- uris filter { u => u.toString().startsWith(prefix) }
      ) yield matching
      candidates.headOption
    }
    val httpURIs = {
      val httpURIs0 = filterURIsByScheme("http")
      httpURIs0 filter { u => !u.toString().startsWith(instanceURIPrefix) }
    }

    val nonDuplicateURI: Rdf#URI =
      //      if (httpURIs.size > 1)
      //        throw new RuntimeException(s"several HTTP URI's: ${uris.mkString(", ")}")
      //      else
      if (httpURIs.size == 0) {
        val uriOption = filterURIsByStartsWith(httpURIs, preferredURIPrefixes)
        uriOption match {
          case None =>
            println(s"""For these HTTP URI's, no preferred URI Prefixes criterion for duplicate: 
              ${uris.mkString(", ")} => first URI kept.""")
            uris(0)
          case Some(uri) => uri
        }
      } else // httpURIs.size >= 1
        httpURIs(0)

    val duplicateURIs = uris diff List(nonDuplicateURI)
    println(s"uriTokeep <$nonDuplicateURI>, duplicates ${duplicateURIs.mkString("<", ">, <", ">")}")

    (nonDuplicateURI, duplicateURIs)
  }

  //  /**
  //   * remove Duplicates for given uri, searching in given class URI,
  //   *  based on identical strings computed by instanceLabel()
  //   */
  //  private def removeDuplicates(uri: Rdf#URI, classURI: Rdf#URI, lang: String = "") = {
  //    val label = instanceLabel(uri, allNamedGraph, lang)
  //    val triples = find(allNamedGraph, ANY, rdf.typ, classURI)
  //    val duplicateURIs = triples.filter {
  //      t =>
  //        t.subject != uri &&
  //          instanceLabel(t.subject, allNamedGraph, lang) == label
  //    }.map { t => t.subject }.toSeq
  //    removeQuadsWithSubjects(duplicateURIs)
  //  }

  /**
   * copy Property Value Pairs from duplicateURIs to uriTokeep,
   * except rdfs:label;
   * includes transaction;
   * @return quads of duplicate URI's
   */
  private def copyPropertyValuePairs(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]) = {
    rdfStore.rw(dataset, {
      val duplicateQuads = for (duplicateURI <- duplicateURIs) yield {
        /* SPARQL query to get the original graph name */
        val quads = quadQuery(duplicateURI, ANY, ANY): Iterable[Quad]
        val triplesToAdd = quads.
        filter { case (t, uri) => t.predicate != prefixesMap2("rdfs")("label") }.
        map {
          // change rdfs:label to indicate merging
          case (t, uri) =>
            val newObject = t.objectt
            (Triple(uriTokeep, t.predicate, newObject), uri)
        }.toList
        // println(s"copyPropertyValuePairs: triplesToAdd $triplesToAdd")
        triplesToAdd.map {
          tripleToAdd =>
            rdfStore.appendToGraph(dataset,
              tripleToAdd._2, Graph(List(tripleToAdd._1)))
        }
        quads .toSeq
      } // . flatMap { x => x }
      duplicateQuads . flatten
      //    println(s"copyPropertyValuePairs: dataset $dataset")
    }) . getOrElse( Seq() )
  }

  /**
   * copy Subject Property Pairs with uriTokeep as object;
   * includes transaction
   */
  private def copySubjectPropertyPairs(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]) = {
    rdfStore.rw(dataset, {
      for (duplicateURI <- duplicateURIs) {
        /* SPARQL query to get the original graph name */
        val quads = quadQuery(ANY, ANY, duplicateURI): Iterable[Quad]
        val triplesToAdd = quads.map {
          case (t, uri) if (true) => (Triple(t.subject, t.predicate, uriTokeep), uri)
        }.toList
        triplesToAdd.map {
          tripleToAdd =>
            rdfStore.appendToGraph(dataset,
              tripleToAdd._2, Graph(List(tripleToAdd._1)))
        }
      }
    })
  }
 
  /** DOES NOT include transactions */
  private def indexInstanceLabels(classURI: Rdf#URI,
                          lang: String): Map[String, Seq[Rdf#URI]] = {
    val classTriples = find(allNamedGraph, ANY, rdf.typ, classURI)
    // NOTE: this looks laborious !!!!
    var count = 0
    val v = for (
      classTriple <- classTriples;
      uri0 = classTriple.subject if (uri0.isURI);
      uri = uri0.asInstanceOf[Rdf#URI];
      label = instanceLabel(uri, allNamedGraph, lang)
    ) yield {
      count = count + 1
      (label, uri)
    }
    val res = v.toList.groupBy(_._1).map {
      case (s, list) => (s,
        list.map { case (s, node) => node })
    }
    println(
      s"indexInstanceLabels: ${count} instances for class $classURI")
    res
  }
  
  def dumpAllNamedGraph(mess: String="") = 
    rdfStore.r( dataset, {
    println(s"$mess: allNamedGraph ${allNamedGraph.toString().
      replaceAll(""";""", ".\n")}" )
    })

    /** Load files into TDB from Args ( starting at args(1) ) */
    def loadFilesFromArgs(args: Array[String]): Array[String] = {
      val files = args.slice(1, args.size)
      println(s"Files ${files.mkString(", ")}")
      for (file <- files) {
        println(s"Load file $file")
        retrieveURI(ops.URI(new File(file).toURI().toASCIIString()))
        println(s"Loaded file $file")
      }
      files
    }
  
  /** output modified data (actually all triples in TDB) in /tmp */
  def outputModifiedTurtle(file: String, outputDir: String = "/tmp" ) = {
    val queryString = """
    CONSTRUCT { ?S ?P ?O }
    WHERE {
      GRAPH ?GR { ?S ?P ?O }
    } """
    val ttl = sparqlConstructQueryTR(queryString)
    val outputFile = outputDir + File.separator + new File(file).getName
    println(s"""Writing ${ttl.length()} chars in output File
      $outputFile""")
    val fw = new FileWriter(new File(outputFile))
    fw.write(ttl)
    fw.close()
  }

  def outputGraph(auxiliaryOutput: Rdf#Graph, file: String, outputDir: String = "." ) = {
    val outputFile = outputDir + File.separator + new File(file).getName
    println(s"""Writing ${auxiliaryOutput.size} triples in output File
      $outputFile""")
    val os = new FileOutputStream(outputFile)
    turtleWriter.write(auxiliaryOutput, os, "")
    os.close()
  }

  /** if class URI == owl:ObjectProperty the rdfs:range's must be equal */
  private def checkRdfsRanges(instanceLabels2URIsMap: Map[String, Seq[Rdf#URI]],
                              classURI: Rdf#URI): Map[String, Seq[Rdf#URI]] = {
    if (classURI == owl.ObjectProperty) {
      instanceLabels2URIsMap.filter {
        pair =>
          val (label, uris) = pair
          val groupedByRdfsRange = uris.groupBy { uri =>
            val ranges = find(allNamedGraph, uri, rdfs.range, ANY).
              map { _.objectt }.toSeq.headOption
            ranges
          }
          groupedByRdfsRange.size == uris.size
      }
    } else instanceLabels2URIsMap
  }
}
