package deductions.runtime.sparql_cache

import deductions.runtime.utils.RDFStoreLocalProvider
import org.w3.banana.RDF
import org.w3.banana.io.{RDFWriter, Turtle}

import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * Browsable Graph implementation, in the sense of
 *  http://www.w3.org/DesignIssues/LinkedData.html
 *
 *  (used for Turtle export)
 */
trait BrowsableGraph[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {

//  val turtleWriter: RDFWriter[Rdf, Try, Turtle]

  /**
   * used in Play! app;
   * NON transactional
   * 
   * @param search : subject or object URI
   * @return
   * all triples <search> ?p ?o   ,
   * plus optionally all triples in graph <search> , plus "reverse" triples everywhere
   */
  def search_only(search: String): Try[Rdf#Graph] = {
    val targetURI = if (search.startsWith("_:"))
      search.replace("_:", "urn:bn:")
    else search
    val queryString =
      s"""
         |CONSTRUCT {
         |  <$targetURI> ?p ?o .
         |  ?thing ?p ?o .
         |  ?s ?p1 <$targetURI> .
         |}
         |WHERE {
         |  GRAPH ?GRAPH {
         |    <$search> ?p ?o .
         |    OPTIONAL {
         |      ?o ?p1 ?s1 .
         |      FILTER( isBlank(?o) )
         |    }
         |  }
         |  OPTIONAL {
         |    GRAPH <$search>
         |    { ?thing ?p ?o . }
         |    GRAPH ?GRAPH2
         |    { ?s ?p1 <$search> . } # "reverse" triples
         |  }
         |}""".stripMargin
    logger.debug("search_only " + queryString)
    val res = sparqlConstructQuery(queryString)
    logger.debug( "search_only: after sparqlConstructQuery" )
    logger.debug(s"search_only: res $res" )
    res
  }

  /** used in Play! app , but blocking ! transactional */
  def focusOnURI(uri: String, mime: String="text/turtle"): String = {
    try {
      val transaction = rdfStore.r( dataset, {
        val triples = search_only(uri)
        triples
      })
      val format = if( mime.contains("turtle"))
        "turtle"
        else if( mime.contains("rdf+xml"))
          "rdfxml"
        else if( mime.contains("n-triples"))
          mime
        else
          "jsonld"

      graph2String(transaction.get, uri, format=format) match {
        case Success(s) => s
        case Failure(f) => f.getLocalizedMessage
      }
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
  }

}
