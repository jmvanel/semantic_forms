package deductions.runtime.data_cleaning

import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.Date

import scala.language.postfixOps
import scala.reflect.io.Path
import scala.util.Try

import org.w3.banana.RDF

import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.services.URIManagement
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.Maps
import scala.util.Success
import scala.util.Failure
import org.apache.commons.lang3.StringUtils

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
    with URIManagement
    with Maps {

	val config: Configuration
  import config._
      import ops._
   
  var originalGraph = emptyGraph

  //  override val databaseLocation: String = "" // in-memory
  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  val deleteDatabaseLocation = true
//  override val config.useTextQuery = false

  /** merge Marker in case of _automatic merge */
  val mergeMarker = " (F)"
  /** merge Marker in case of Specification based merge */
  val mergeMarkerSpec = " (FS)"
  val globalNamedGraph = URI("urn:/globalNamedGraph")

  type URIMergeSpecifications = List[URIMergeSpecification]
  case class URIMergeSpecification(replacedURI: Rdf#URI, replacingURI: Rdf#URI,
    newLabel: String = "", comment: String = "") {
	  def isRenaming() = newLabel != ""
	  def isreplacing() = replacedURI != nullURI
	}

  /**
   * merges Duplicates automatically among instances of given class URI,
   * based on criteria : instanceLabel() giving identical result;
   * moreover if class URI == owl:ObjectProperty the rdfs:range's must be equal
   * includes transactions
   * @return execution report
   */
  def removeAllDuplicates(classURI: Rdf#URI, lang: String = ""): String = {

    val instanceLabels2URIsMap =
      rdfStore.rw(dataset, {
        indexInstanceLabels(classURI, lang)
      }).get

    var duplicatesCount = 0
    var propertiesHavingDuplicates = 0

    println(s"instanceLabels2URIsMap size ${instanceLabels2URIsMap.size}")
    val instanceLabels2URIsMap2 = rdfStore.rw(dataset, {
      checkRdfsRanges(instanceLabels2URIsMap, classURI)
    }).get
    println(s"instanceLabels2URIsMap 2, after checkRdfsRanges: size ${instanceLabels2URIsMap2.size}")

    for( (label, allDuplicateURIs) <- instanceLabels2URIsMap2) {
      println(s"""\n>>>> Looking at label "$label" """)
      try {
        val (uriTokeep, duplicateURIs) = tellURItoKeepAmongDuplicates(allDuplicateURIs)
        if (!duplicateURIs.isEmpty) {
          propertiesHavingDuplicates = propertiesHavingDuplicates + 1
          duplicatesCount = duplicatesCount + duplicateURIs.size
          println(s"""Deleting duplicates for "${label}" uriTokeep <$uriTokeep>, delete count ${duplicateURIs.size}
            ${duplicateURIs.mkString("<", ">, <" ,">")}""")
          val ( convergenceURI, duplicateURIs2 ) = managePriorMerges(uriTokeep, duplicateURIs)
          val named_graph = removeDuplicatesFromSeq(convergenceURI, duplicateURIs2)
          storeLabelWithMergeMarkerTR(convergenceURI, merge_marker = mergeMarker,
            graphToWrite = named_graph)

          addRestructuringComment( convergenceURI, duplicateURIs2 )
        }
      } catch {
        case t: Throwable =>
          println("WARNING: removeAllDuplicates: " + t.getClass + " " + t.getLocalizedMessage)
      }
    }
    s"propertiesHavingDuplicates: $propertiesHavingDuplicates, duplicatesCount: $duplicatesCount"
  }

  /**
   * remove Duplicates for 1 URI To keep, from possibly multiple merge Specifications:
   * - calling [[removeDuplicatesFromSeq]] on given merge Specifications,
   * - replace multiple rdfs:labels with the given new label from mergeSpecifications;
   * - add a merge Marker to rdfs:label's
   */
  def removeDuplicatesFromSpec(uriTokeep: Rdf#URI,
                               mergeSpecifications0: URIMergeSpecifications,
                               auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()): Unit = {

    println(s"\nuriTokeep <$uriTokeep>")
    for (mergeSpecification <- mergeSpecifications0) {
      val assertion = mergeSpecification.replacedURI != "" &&
        mergeSpecification.replacingURI != ""
      println(s"mergeSpecification $mergeSpecification assertion $assertion")
      System.out.flush() ; if (!assertion) System.exit(0)
    }
    println("!!!! removeDuplicatesFromSpec: checked mergeSpecification's")

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

    if( uriTokeep.toString().contains("OngletAcces/conditionAcces/procedureAcces/cursusRequis"))
      println(s">>>> 2:OngletAcces/conditionAcces/procedureAcces/cursusRequis") // DEBUG <<<<<<<<<<<<<<<<<<<<<<<<<<
      
    //  create and replace triples for new rdfs:label

    val newLabels = for (
      mergeSpecification <- mergeSpecifications;
      newLabel = mergeSpecification.newLabel;
      if (newLabel != "")
    ) yield mergeSpecification.newLabel
    if (newLabels.size > 1)
      System.err.println(s"WARNING! several new Labels for $uriTokeep: $newLabels")
    val newLabel = newLabels.headOption.getOrElse("")

    val transaction = rdfStore.rw(dataset, {
      println(s"Before storeLabelWithMergeMarker: size ${allNamedGraph.size}")
      storeLabelWithMergeMarker(uriTokeep, merge_marker = mergeMarkerSpec,
        graphToWrite = named_graph, newLabel = newLabel)

      for (mergeSpecification <- mergeSpecifications) {
        // recycle old rdfs:label's as skos:altLabel's
        if (mergeSpecification.replacedURI != mergeSpecification.replacingURI) {
          val labelsFromReplacedURI: List[Quad] = removeFromQuadQuery(mergeSpecification.replacedURI, rdfs.label, ANY)
//          if (!labelsFromReplacedURI.isEmpty) {
            val newTriples = for (removedQuad <- labelsFromReplacedURI)
              yield Triple(mergeSpecification.replacingURI, skos("altLabel"), removedQuad._1.objectt);
            rdfStore.appendToGraph(dataset, nodeToURI(named_graph), makeGraph(newTriples))
//          }
        }

        // add rdfs:comment from given merge Specification
        if (mergeSpecification.comment != "") {
          val commentTriple = Triple(mergeSpecification.replacingURI,
            rdfs.comment, Literal(mergeSpecification.comment))
          rdfStore.appendToGraph(dataset, nodeToURI(named_graph), makeGraph(List(commentTriple)))
        }
      }
      addRestructuringCommentNoTr(uriTokeep, duplicateURIs, mergeMarkerSpec)
    })
    println(s"After storeLabelWithMergeMarker: transaction $transaction")
    rdfStore.rw(dataset, {
    println(s"After storeLabelWithMergeMarker: size ${allNamedGraph.size}")
    })
  }

  /**
   * add a merge Marker to rdfs:label's;
   *  replaces existing triple(s)
   *  <replacingURI> rdfs.label ?LAB
   *
   * NEEDS Transaction
   */
  private[data_cleaning] def storeLabelWithMergeMarker(
    uri: Rdf#Node,
    newLabel: String = "",
    merge_marker: String = mergeMarker,
    graphToWrite: Rdf#Node = URI("")) = {
    if (uri.toString() != "") {
      val label = if (newLabel != "") {
        newLabel
      } else {
        println(s"storeLabelWithMergeMarker: WARNING: no new label provided for URI <$uri> , taking its rdfs:label")
        implicit val graph = allNamedGraph: Rdf#Graph
        getStringHeadOrElse(uri, rdfs.label)
      }
      if (label != "") {
        val newLabelTriple = Triple(uri, rdfs.label, Literal(label + merge_marker))
//        replaceRDFTriple(newLabelTriple, graphToWrite, dataset)
        replaceRDFTripleAnyGraph(newLabelTriple, dataset)
      }
    }
  }

  /**
   * add a merge Marker to rdfs:label's;
   *  replaces existing triple(s) <replacingURI> rdfs.label ?LAB
   *  
   * WITH Transaction
   */
  private def storeLabelWithMergeMarkerTR(replacingURI: Rdf#Node,
                                          newLabel: String = "",
                                          merge_marker: String = mergeMarker,
                                          graphToWrite: Rdf#Node = URI("")) = {
    val transaction = rdfStore.rw(dataset, {
      storeLabelWithMergeMarker(replacingURI,
        newLabel,
        merge_marker,
        graphToWrite)
    })
  }

  /**
   * add restructuring comment (annotation property),
   *  telling with URI's have been merged
   *  and other traceabilility items: restruc:oldLabel , restruc:mergedFrom ;
   *  called both for merging with and without specification
   *  DOES NOT include transaction
   */
  private def addRestructuringCommentNoTr(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node],
                                          comment: String = mergeMarker,
                                          graphToWrite: Rdf#URI = URI("")) = {

    val restructurationCommentTriple = {

      val dupsComment = for (
        duplicateURI <- duplicateURIs
      ) yield {
        val oldLabel = instanceLabel(duplicateURI, originalGraph, "fr")
        abbreviateTurtle(duplicateURI) + s" ($oldLabel)"
      }
      val restructurationComment =
        s"""
          |Fusion $comment le ${new Date}
          ||vers $uriTokeep
          |à partir de
          |${dupsComment.mkString(",\n")}
    """.stripMargin
      val restrucProp = restruc("restructurationComment")
      Triple(
        uriTokeep,
        restrucProp,
        Literal(restructurationComment))
    }

    val newLabel = {
      val groupLabel = instanceLabel(uriTokeep, originalGraph, "fr")
      val mergedItemTripleOption = find(allNamedGraph, ANY, restruc("oldLabel"), Literal(groupLabel)).toList
      val label = mergedItemTripleOption.headOption.map {
        mergedItemTriple =>
          val mergedURI = mergedItemTriple.subject
          val label = instanceLabel(mergedURI, originalGraph, "fr")
          println(s""">>>> addRestructuringCommentNoTr: newLabel found for <$uriTokeep> from merged URI <$mergedURI> : "${label}" """)
          label
      }
      label.getOrElse(groupLabel)
    }
    val dupsTriples = {
      for (
        duplicateURI <- duplicateURIs;
        oldLabel = instanceLabel(duplicateURI, originalGraph, "fr") if (oldLabel != newLabel)
      ) yield {
        List( Triple(uriTokeep, restruc("mergedFrom"),duplicateURI ),
        Triple(uriTokeep, restruc("oldLabel"), Literal(oldLabel)) )
      }
    } . flatten

    val newLabelTriple = Triple(uriTokeep, restruc("newLabel"), Literal(newLabel))

    val tripleList = newLabelTriple :: restructurationCommentTriple :: dupsTriples.toList

    rdfStore.appendToGraph(dataset, graphToWrite,
      makeGraph(tripleList))
  }

  /** detect prior merges by specification
   *  @return the convergence URI, not necessarily `uriTokeep` */
  def managePriorMerges(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node]): (Rdf#Node, Seq[Rdf#Node] ) = {
		  val groupLabel = instanceLabel(uriTokeep, originalGraph, "fr")
      val mergedItemTripleOption = find(originalGraph, ANY, restruc("oldLabel"), Literal(groupLabel)).toList
      println( s""">>>> managePriorMerges: uriTokeep $uriTokeep groupLabel "$groupLabel" mergedItemTripleOption ${mergedItemTripleOption}""" )
      val opt = mergedItemTripleOption.headOption.map {
        mergedItemTriple =>
        mergedItemTriple.subject
      }
		  if( opt.isDefined ) println( s">>>> managePriorMerges: opt $opt merges ${duplicateURIs.mkString("<", ">, <", ">")}" )
		  val duplicateURIs2 = if( opt.isDefined ) uriTokeep +: duplicateURIs else duplicateURIs
      ( opt.getOrElse(uriTokeep), duplicateURIs2 )
  }

  private def addRestructuringComment(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node],
        comment: String=mergeMarker,
        graphToWrite: Rdf#URI=URI("")) = {    
    val transaction = rdfStore.rw(dataset, {
      addRestructuringCommentNoTr(uriTokeep, duplicateURIs, comment, graphToWrite)
    })
  }

  /**
   * generic algorithm to remove Duplicates in given duplicate URI's
   *  the algorithm:
   *  <pre>
   * - from triples &lt;duplicateURIs> ?P ?O
   *   create triples &lt;uriTokeep> ?P ?O
   *   delete triples &lt;duplicateURIs> ?P ?O
   *
   * - from reverse triples ?S ?P1 &lt;duplicateURIs>
   *   create triples ?S ?P1 &lt;uriTokeep>
   *   delete triples ?S ?P1 &lt;duplicateURIs>
   *  </pre>
   *
   * includes transactions
   * @return named graph 
   */
  private def removeDuplicatesFromSeq(
    uriTokeep: Rdf#Node,
    duplicateURIs: Seq[Rdf#Node],
    auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()): Rdf#Node = {

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
  protected def tellURItoKeepAmongDuplicates(uris: Seq[Rdf#Node]): (Rdf#Node, Seq[Rdf#Node]) = {
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
    def filterURIsByStartsWith(uris: Seq[Rdf#Node], prefixes: Seq[String]): Option[Rdf#Node] = {
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

    val nonDuplicateURI: Rdf#Node =
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

  /**
   * copy Property Value Pairs from duplicateURIs to uriTokeep,
   * except rdfs:label;
   * includes transaction;
   * @return quads of duplicate URI's
   */
  private def copyPropertyValuePairs(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node]) = {
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
  private def copySubjectPropertyPairs(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node]) = {
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
 
  /** index Instance by Labels (for merging Duplicates automatically);
   * @return a map of Instance Labels to sequences of URI
   * DOES NOT include transactions */
  private def indexInstanceLabels(classURI: Rdf#URI,
                          lang: String): Map[String, Seq[Rdf#Node]] = {
    val classTriples = find(allNamedGraph, ANY, rdf.typ, classURI) . toList
    println( s"indexInstanceLabels: ${classTriples.size} instances for class $classURI")

    // make (label, uri) pairs; NOTE: this looks laborious !!!!
    var count = 0
    val labelURIpairs = for (
      classTriple <- classTriples;
      uri0 = classTriple.subject if (uri0.isURI);
      uri = uri0 ;
      label = instanceLabel(uri, allNamedGraph, lang)
    ) yield {
      count = count + 1
      (label, uri)
    }
    // group By similar labels (strip Accents)
    val labelsToURIsmap = labelURIpairs.toList.groupBy {
      pair => StringUtils.stripAccents(pair._1)
    } . map {
      case (s, list) => (s,
        list.map { case (s, node) => node })
    }
    println( s"indexInstanceLabels: ${labelsToURIsmap.size} different labels in instances for class <$classURI>")

    val mergedItemsList = labelsToURIsmap.map {
      case (groupLabel, list) =>
        list.map {
          uri =>
            val mergedItemTripleOption = find(allNamedGraph, ANY, restruc("oldLabel"), Literal(groupLabel)).toList
            mergedItemTripleOption.headOption.map {
              mergedItemTriple =>
                val mergedURI = mergedItemTriple.subject
                (groupLabel, mergedURI)
            }
        }
    }
    val r2 = mergedItemsList . flatten .toList
    val mergedItemsMap: Map[String, List[Rdf#Node]] = r2 . map {
      case Some((label, uri)) => (label, List( uri))
      case _ => ( "", List(nullURI ))
    } . toMap

//    println( s"DDDDDDDDDDD indexInstanceLabels: Auteur: ${res.getOrElse("Auteur", "")} ")
//    mergeMapsOfLists(labelsToURIsmap, mergedItemsMap)
    labelsToURIsmap
  }
  
  def dumpAllNamedGraph(mess: String="") = 
    rdfStore.r( dataset, {
    println(s"$mess: allNamedGraph ${allNamedGraph.toString().
      replaceAll(""";""", ".\n")}" )
    })
 
    /** Load files into TDB from Args ( starting at args(1) ) */
    def loadFilesFromArgs(args: Array[String],from: Int=1)
//  : Array[String]
  = {
      val files = args.slice(from, args.size)
      println(s"Files ${files.mkString(", ")}")
      val uris = for (file <- files) yield {
        println(s"Load file $file")
        val uri = uriFromFile(file)
        readStoreURI( URI(uri), globalNamedGraph, dataset) // : Rdf#Graph
        println(s"Loaded file $file => $uri")
        uri
      }
      val r = rdfStore.rw(dataset, {
    	  originalGraph = union(Seq(allNamedGraph))
        // println(s"originalGraph size ${originalGraph.size()(ops)}")
      })
      files
    }
  
  /* TODO move to global utility, and use Jena utility */
  def uriFromFile(filename: String) =
             org.apache.jena.riot.system.IRIResolver.resolveFileURL(filename)
  //    new File(file).toURI().toASCIIString()

  /** output modified data (actually all triples in TDB) in /tmp */
  def outputModifiedTurtle(file: String, outputDir: String = "/tmp", suffix: String = "") = {
    val queryString = """
    CONSTRUCT { ?S ?P ?O }
    WHERE {
      GRAPH ?GR { ?S ?P ?O }
    } """
    val tryTTL = sparqlConstructQueryTR(queryString)
    tryTTL match {
      case Success(ttl) =>
        val outputFile = outputDir + File.separator + new File(file).getName + suffix
        println(s"""Writing ${ttl.toString.length()} chars in output File
          $outputFile""")
        val fw = new FileWriter(new File(outputFile))
        fw.write(ttl.toString())
        fw.close()
      case Failure(f) => System.err.println(s"Error: $f")
    }
  }

  def outputGraph(auxiliaryOutput: Rdf#Graph, file: String, outputDir: String = "." ) = {
    val outputFile = outputDir + File.separator + new File(file).getName
    println(s"""Writing ${auxiliaryOutput.size} triples in output File
      $outputFile""")
    val os = new FileOutputStream(outputFile)
    turtleWriter.write(auxiliaryOutput, os, "")
    os.close()
  }

  /** if class URI == owl:ObjectProperty, the rdfs:range's must be equal */
  private def checkRdfsRanges(instanceLabels2URIsMap: Map[String, Seq[Rdf#Node]],
                              classURI: Rdf#URI): Map[String, Seq[Rdf#Node]] = {
    if (classURI == owl.ObjectProperty) {
      instanceLabels2URIsMap.filter {
        case (label, uris) =>
          if( label == "Parcours d'accès au métier" )
        	  println(s""">>>> checkRdfsRanges: label "Parcours d'accès au métier" """) // DEBUG <<<<<<<<<<<<<<<<<<<<<<<<<<

//          if(label == "Auteur") println( s"DDDDDDDDDDD checkRdfsRanges: Auteur 2 : ${uris}")
          val groupedByRdfsRange = uris.groupBy { uri =>
            val ranges = find(allNamedGraph, uri, rdfs.range, ANY).
              map { _.objectt }.toSeq.headOption
            ranges
          }
          groupedByRdfsRange.size == 1 // uris.size
      }
    } else instanceLabels2URIsMap
  }

  protected def possiblyDeleteDatabaseLocation() = {
    Try {
      val path = Path(databaseLocation)
      if (deleteDatabaseLocation) {
        path.deleteRecursively()
        println(s"reset database Location $databaseLocation")
      }
    }
  }

}
