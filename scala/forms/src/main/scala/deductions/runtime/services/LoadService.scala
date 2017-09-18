package deductions.runtime.services

import java.io.StringReader

import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF
import org.w3.banana.io.{JsonLd, RDFReader}

import scala.util.{Failure, Success, Try}

/** service accepting JSON-LD;
 * HTTP POST with body (JSON-LD data), and
 * parameter graph ( default data:/geo/ ),
 * compliant to SPARQL 1.1 Graph Store HTTP Protocol (except no conneg yet)
 * https://www.w3.org/TR/sparql11-http-rdf-update/
 * */
trait LoadService[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
{

  import ops._
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  /** load RDF String in database - TODO conneg !!! */
  def load(request: HTTPrequest ) = {
    val httpParamsMap: Map[String, Seq[String]] = request.queryString
    println( s"httpParamsMap $httpParamsMap" )
    val jsonLDdataOption = request.content
    val graphURI = httpParamsMap.getOrElse("graph", "data:/geo/").toString()
    
    // database: String = "TDB"    TODO

    val jsonLDdataEx = jsonLDdataOption match {
      case Some(s) => Success(s)
      case _ => Failure(new Exception("load: No content in request"))
    }

    val v = for (
      jsonLDdata <- jsonLDdataEx ;
      graph <- jsonldReader.read(new StringReader(jsonLDdata), graphURI) ;
      dummy = println(s"After read graph $graph")
    ) yield {
      graph
    }

    println(s">>>> Before storeURI graphURI <$graphURI> ${v.toString}")
    val gr = storeURI(v, URI(graphURI), dataset)
    println(s">>>> After storeURI ${gr.toString}")
    gr
  }
}
