package deductions.runtime.services

import org.w3.banana.RDF
import org.w3.banana.io.RDFReader
import scala.util.Try
import org.w3.banana.io.JsonLd
import java.io.StringReader
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.HTTPrequest

/** service producing the raw form syntax in JSON */
trait LoadService[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
{

  import ops._
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  def load(request: HTTPrequest ) = {
    val httpParamsMap: Map[String, Seq[String]] = request.queryString
    println( s"httpParamsMap $httpParamsMap" )
    val dataArgs = httpParamsMap.getOrElse("data", Seq())    
    val jsonLDdata = dataArgs(0)
    val graphURI = httpParamsMap.getOrElse("graphURI", "data:/geo/").toString()
    
    // database: String = "TDB"    TODO

    val v = for {
      graph <- jsonldReader.
        read(new StringReader(jsonLDdata), graphURI)
      dummy = println(s"After read graph $graph")
    } yield {
      graph
    }
    println(s">>>> Before storeURI graphURI <$graphURI> ${v.toString}")
    val gr = storeURI(v, URI(graphURI), dataset)
    println(s">>>> After storeURI ${gr.toString}")
    gr
  }
}