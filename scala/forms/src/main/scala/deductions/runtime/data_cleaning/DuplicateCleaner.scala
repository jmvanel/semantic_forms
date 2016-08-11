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
import java.net.URI
import deductions.runtime.services.URIManagement
import java.io.FileWriter
import java.io.File
import deductions.runtime.sparql_cache.RDFCacheAlgo

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
    with URIManagement
{
  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

  /**
   * merges Duplicates among instances of given class URI,
   * based on criterium: instanceLabel() giving identical result;
   *  includes transactions
   */
  def removeAllDuplicates(classURI: Rdf#URI, lang: String = ""): String = {
    val instanceLabels2URIsMap: Map[String, Seq[Rdf#URI]] =
      rdfStore.rw(dataset, {
        indexInstanceLabels(classURI, lang)
      }).get

    var duplicatesCount = 0
    var propertiesHavingDuplicates = 0

    for (el <- instanceLabels2URIsMap) {
      println(s"""looking at label "${el._1}" """)
      try {
        val (uriTokeep, duplicateURIs) = tellDuplicates(el._2)
        println(s"uriTokeep <$uriTokeep>, duplicates ${duplicateURIs.mkString("<", ">, <", ">")}")

        if (!duplicateURIs.isEmpty) {
          propertiesHavingDuplicates = propertiesHavingDuplicates + 1
          duplicatesCount = duplicatesCount + duplicateURIs.size
          println(s"""Deleting duplicates for "${el._1}" uriTokeep <$uriTokeep>, delete count ${duplicateURIs.size}""")
          removeDuplicates(uriTokeep, duplicateURIs )
        }
      } catch {
        case t: Throwable =>
          println("WARNING: removeAllDuplicates: " + t.getClass + " " + t.getLocalizedMessage)
      }
    }
    s"propertiesHavingDuplicates: $propertiesHavingDuplicates, duplicatesCount: $duplicatesCount"
  }

  /** the algorithm:
   * - from triples <duplicateURIs> ?P ?O
   *   create triples <uriTokeep> ?P ?O
   * - delete triples <duplicateURIs> ?P ?O
   *
   * - from reverse triples ?S ?P1 <duplicateURIs>
   *   create triples ?S ?P1 <uriTokeep>
   * - delete triples ?S ?P1 <duplicateURIs>
   * */
  def removeDuplicates(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]): Unit = {
    copyPropertyValuePairs(uriTokeep, duplicateURIs)
    copySubjectPropertyPairs(uriTokeep, duplicateURIs)
    if( duplicateURIs.contains(uriTokeep) ) {
      println( s"CAUTION: duplicateURIs.contains(uriTokeep=$uriTokeep)")
      removeQuadsWithSubjects(duplicateURIs.toList.diff(List(uriTokeep)))
      removeQuadsWithObjects(duplicateURIs.toList.diff(List(uriTokeep)))
    } else {
      removeQuadsWithSubjects(duplicateURIs)
      removeQuadsWithObjects(duplicateURIs)
      println(s"Deleted ${duplicateURIs.size} duplicate URI's for $uriTokeep")
    }
    processKeepingTrackOfDuplicates(uriTokeep, duplicateURIs)
    processMultipleRdfsDomains(uriTokeep, duplicateURIs)
  }
    
  /** includes transaction */
  protected def removeQuadsWithSubjects(subjects: Seq[Rdf#Node]) = {
    for (dupURI <- subjects) {
      val transaction = rdfStore.rw( dataset, {
        removeQuadsWithSubject(dupURI)
      })
    }
  }

  /** includes transaction */
  protected def removeQuadsWithObjects(objects: Seq[Rdf#Node]) = {
    for (dupURI <- objects) {
      val transaction = rdfStore.rw( dataset, {
        removeQuadsWithObject(dupURI)
      })
    }
  }

  /**
   * tell which URI's are Duplicates;
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
   * @return the URI considered as non-duplicate, and a list of the duplicates
   */
  protected def tellDuplicates(uris: Seq[Rdf#URI]): (Rdf#URI, Seq[Rdf#URI]) = {
    if (uris.size <= 1)
      // nothing to do by caller! no Duplicates
      return (ops.URI(""), Seq())

    def filterURIsByScheme(scheme: String) = {
      uris.filter { uri =>
        val jURI = new URI(uri.toString())
        jURI.getScheme == scheme
      }
    }
    /** @return the first URI matching one of the prefixes. */
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
      } else // httpURIs.size == 1
        httpURIs(0)

    (nonDuplicateURI, uris diff List(nonDuplicateURI))
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
   * copy Property Value Pairs;
   * includes transaction
   */
  private def copyPropertyValuePairs(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]) = {
    for (duplicateURI <- duplicateURIs) {
      rdfStore.rw(dataset, {
        /* SPARQL query to get the original graph name */
        val quads = quadQuery(duplicateURI, ANY, ANY): Iterable[Quad]
        val triplesToAdd = quads.map {
          case (t, uri) => (Triple(uriTokeep, t.predicate, t.objectt), uri)
        }.toList
//        println(s"copyPropertyValuePairs: triplesToAdd $triplesToAdd")
        triplesToAdd.map {
          tripleToAdd =>
            rdfStore.appendToGraph(dataset,
              tripleToAdd._2, Graph(List(tripleToAdd._1)))
        }
      })
    }
//    println(s"copyPropertyValuePairs: dataset $dataset")
  }

  /**
   * copy Subject Property Pairs;
   * includes transaction
   */
  private def copySubjectPropertyPairs(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]) = {
    for (duplicateURI <- duplicateURIs) {
      rdfStore.rw(dataset, {
        /* SPARQL query to get the original graph name */
        val quads = quadQuery(ANY, ANY, duplicateURI): Iterable[Quad]
        val triplesToAdd = quads.map {
          case (t, uri) => (Triple(t.subject, t.predicate, uriTokeep), uri)
        }.toList
        triplesToAdd.map {
          tripleToAdd =>
            rdfStore.appendToGraph(dataset,
              tripleToAdd._2, Graph(List(tripleToAdd._1)))
        }
      })
    }
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
  
  /** output modified data in /tmp */
  def outputModifiedTurtle(file: String) = {
    val queryString = """
    CONSTRUCT { ?S ?P ?O }
    WHERE {
      GRAPH ?GR { ?S ?P ?O }
    } """
    val ttl = sparqlConstructQueryTR(queryString)
    val outputFile = "/tmp/" + new File(file).getName
    println(s"Writing ${ttl.length()} chars in  output File $outputFile")
    val fw = new FileWriter(new File(outputFile))
    fw.write(ttl)
    fw.close()
  }
}
