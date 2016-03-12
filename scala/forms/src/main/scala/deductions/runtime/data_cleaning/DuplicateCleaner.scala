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
import deductions.runtime.sparql_cache.BlankNodeCleanerBase
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import java.net.URI
import deductions.runtime.services.URIManagement

/**
 * merge FOAF duplicates
 *  #41 https://github.com/jmvanel/semantic_forms/issues/41
 */
trait DuplicateCleaner[Rdf <: RDF, DATASET]
    extends BlankNodeCleanerBase[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with URIManagement {
  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

  //  val rdf = RDFPrefix[Rdf]

  def removeAllDuplicates(classURI: Rdf#URI, lang: String = "") = {
    val instanceLabels2URIsMap: Map[String, Seq[Rdf#Node]] = indexInstanceLabels()
    for (el <- instanceLabels2URIsMap) {
      println(s"looking at $el._1")
      val (uriTokeep, duplicateURIs) = tellDuplicates(el._2)
      copyPropertyValuePairs(uriTokeep, duplicateURIs)
      removeQuadsWithSubjects(duplicateURIs)
    }
  }

  def removeQuadsWithSubjects(subjects: Seq[Rdf#Node]) = {
    for (dupURI <- subjects) {
      val transaction = dataset.r({
        removeQuadsWithSubject(dupURI)
      })
    }
  }

  /**
   * tell which URI's are Duplicates;
   *  NOTE: this is the function you may want to override, depending on your URI creation policy.
   *
   * The URI considered as non-duplicate will be
   * HTTP URI's not in the DSN domain of the server
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
  def tellDuplicates(uris: Seq[Rdf#Node]): (Rdf#Node, Seq[Rdf#Node]) = {
    if (uris.size <= 1)
      // nothing to do by caller!
      return (ops.URI(""), Seq())

    def filterURIsByScheme(scheme: String) = {
      uris.filter { uri =>
        val jURI = new URI(uri.toString())
        jURI.getScheme == scheme
      }
    }
    def filterURIsByStartsWith( uris: Seq[Rdf#Node], prefixes: Seq[String]) = {
      val v = for( prefix <- prefixes ) {
    	  uris filter { u => u.toString().startsWith(prefix) }
      }
    }
    
    val httpURIs = {
      val httpURIs0 = filterURIsByScheme("http")
      httpURIs0 filter { u => !u.toString().startsWith(instanceURIPrefix) }
    }

    val nonDuplicateURI =
      if (httpURIs.size > 1)
        throw new RuntimeException(s"several HTTP URI's: ${uris.mkString(", ")}")
      else if (httpURIs.size == 0) {
        val urnURIs = filterURIsByScheme("urn")
        if (urnURIs.size > 0)
          filterURIsByStartsWith TODO <<<<<<<<<<<<<<<<<<<<<<<<<<<<
          // preferredURIPrefixes
          urnURIs(0)
        else
        	println( s"For these HTTP URI's, the case is not foressen: ${uris.mkString(", ")}")
        	httpURIs(0)
      } else if (httpURIs.size == 1) {
        httpURIs(0)
      }
    ???
  }

  /**
   * remove Duplicates for given uri, searching in given class URI,
   *  based on identical strings computed by instanceLabel()
   */
  def removeDuplicates(uri: Rdf#URI, classURI: Rdf#URI, lang: String = "") = {

    //      instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    val label = instanceLabel(uri, allNamedGraph, lang)

    val triples = find(allNamedGraph, ANY, rdf.typ, classURI)
    val duplicateURIs = triples.filter {
      t =>
        t.subject != uri &&
          instanceLabel(t.subject, allNamedGraph, lang) == label
    }.map { t => t.subject }.toSeq

    removeQuadsWithSubjects(duplicateURIs)
  }

  def copyPropertyValuePairs(uriTokeep: Rdf#Node, duplicateURIs: Seq[Rdf#Node]) = {
    ???
  }

  def indexInstanceLabels() = {
    ???
  }
}