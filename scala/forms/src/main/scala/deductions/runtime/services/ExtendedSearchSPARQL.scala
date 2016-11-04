package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.NodeSeq

import deductions.runtime.utils.RDFPrefixes

/**
 * Broader "interest" search in simple search results page
 *  TESTED with
 *  http://localhost:9000/esearch?q=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
 */
trait ExtendedSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  def extendedSearch(uri: String, hrefPrefix: String = ""): Future[NodeSeq] =
    search(uri, hrefPrefix)

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String): String = {
      val q = s"""
       |${declarePrefix(foaf)}
       |SELECT DISTINCT ?thing WHERE {
       |  graph ?g {
       |    # "backward" links distance 2
       |    ?TOPIC ?PRED <$search> .
       |    ?thing ?PRED2  ?TOPIC .
       |    # ?S a foaf:Person .
       |} OPTIONAL {
       |  graph ?g {
       |    # "forward-backward" links distance 2
       |    <$search> ?PRED3 ?TOPIC2 .
       |    ?thing ?PRED4 ?TOPIC2 .
       |    # ?S a foaf:Person .
       |  }
       |}
       | OPTIONAL {
       |  graph ?g {
       |    # "forward" links distance 2
       |    <$search> ?PRED4 ?TOPIC3 .
       |    ?TOPIC3 ?PRED5 ?thing .
       |  }
       |}
       |}
""".stripMargin
      println("extendedSearch: query: " + q)
      q
    }
  }

}
