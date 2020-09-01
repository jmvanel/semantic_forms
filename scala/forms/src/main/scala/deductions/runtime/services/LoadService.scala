package deductions.runtime.services

import java.io.StringReader

import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF
import org.w3.banana.io.{ JsonLd, RDFReader }

import scala.util.{ Failure, Success, Try }

/**
 * service accepting JSON-LD;
 * HTTP POST with body (JSON-LD data), and
 * parameter graph ( default data:/geo/ ),
 * compliant to SPARQL 1.1 Graph Store HTTP Protocol (with conneg)
 * https://www.w3.org/TR/sparql11-http-rdf-update/
 */
trait LoadService[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFContentNegociationIO[Rdf, DATASET] {

  import ops._

  /** load RDF String in database - conneg ! */
  def load(request: HTTPrequest) = {
    val httpParamsMap: Map[String, Seq[String]] = request.queryString
    println(s"load: httpParamsMap $httpParamsMap")
    val anyRDFdataOption = request.content
    val graphURI = request.getHTTPparameterValue("graph").getOrElse("data:/geo/")

    // database: String = "TDB"    TODO

    val anyRDFdataEx: Try[String] = anyRDFdataOption match {
      case Some(s) => Success(s)
      case _       => Failure(new Exception("load: No content in request"))
    }

    val rdfReader = getRDFReader(request)
    // tryGraph
    val tryGraph: Try[Rdf#Graph] = for (
      anyRDFdata <- anyRDFdataEx;
      graph <- rdfReader.read(new StringReader(anyRDFdata), graphURI);
      dummy = logger.info(s"After read graph ${substringSafe(graph.toString(), 100)} ....")
    ) yield {
      graph
    }

    logger.info(s""">>>> load: Before storeURI graphURI <$graphURI> , rdfReader $rdfReader,
      triples ${substringSafe(tryGraph.toString, 150)} ...""")
    val ret = wrapInTransaction {
      storeURINoTransaction(tryGraph, URI(graphURI), dataset)
//    val gr = storeURI(tryGraph, URI(graphURI), dataset)
      syncTDB()
    }
    logger.info(s">>>> load: After storeURI graphURI <$graphURI> , freeMemory ${Runtime.getRuntime.freeMemory()}")
    ret
  }

}
