package deductions.runtime.services.html

import deductions.runtime.abstract_syntax.FormSyntaxJson
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFTreeDuplicator
import deductions.runtime.core.HTTPrequest

import org.w3.banana.RDF

import java.net.URLDecoder

trait PrefillForm[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with RDFPrefixes[Rdf]
with RDFTreeDuplicator[Rdf]
with FormSyntaxJson[Rdf]
{
  import ops._

  /** create Prefilled input Form, from the Referer URL */
  def createPrefillForm(form: FormSyntax, request: HTTPrequest) : FormSyntax = {
    import request._
    val referer = getHTTPheaderValue("Referer") getOrElse("")
    val referenceSubjectURI = URLDecoder.decode( substringAfter( referer, config.hrefDisplayPrefix() ), "UTF-8")
    // Referer example: http://localhost:9000/display?displayuri=http%3A%2F%2F172.17.0.1%3A9000%2Fldp%2FHerv%C3%A9_Mureau
    logger.debug(s""">>>> createPrefillForm: referenceSubjectURI $referenceSubjectURI
        request.path ${request.path}""")
    if( referenceSubjectURI != "" &&
        path == "/create" &&
        getHTTPparameterValue("prefill").getOrElse("").trim() != "no" )
    wrapInReadTransaction {
      val triplesWithValuesToCopy = duplicateTree(makeUri(referenceSubjectURI), form.subject, allNamedGraph) . toList
//      println ( s"createPrefillForm: triplesWithValuesToCopy \n${triplesWithValuesToCopy.mkString("\t\n")}")
//      println ( s"createPrefillForm: form.fields.size ${form.fields.size} :::: \n${form.fields.mkString("\n")}")
      val newFfields = for (field <- form.fields) yield {
//        println ( s"createPrefillForm: form field ${field}")
        val found = triplesWithValuesToCopy.find(triple => triple.predicate == makeURI(field.property))
//        println ( s"createPrefillForm: found $found")
        val f = found match {
          case Some(t) if( reallyPrefillProperty(t.predicate) ) => field.copyEntry(value = t.objectt)
          case _ => field
        }
//        println ( s"createPrefillForm: newFfield $f")
        f
      }
//      println ( s"createPrefillForm: 3 form.fields.size ${form.fields.size} :::: ${form.fields.mkString("\n")}")
      form.fields = newFfields
    }
    form
  }
 
  private def reallyPrefillProperty(prop: Rdf#URI): Boolean = {
    val noPrefillProperties: List[Rdf#URI] =
      List( geo("lat"), geo("long"), geo("alt"),
            URI("http://deductions.github.io/nature_observation.owl.ttl#taxon") )
    println ( s"reallyPrefillProperty: <$prop> ${! noPrefillProperties.contains(prop)}")
    ! noPrefillProperties.contains(prop)
  }
}