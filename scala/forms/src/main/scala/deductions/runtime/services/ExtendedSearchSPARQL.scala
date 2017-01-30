package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.NodeSeq

import org.w3.banana.RDF

import deductions.runtime.utils.RDFPrefixes

/**
 * Broader "interest" search in simple search results page
 *  TESTED with
 *  http://localhost:9000/esearch?q=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
 */
trait ExtendedSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
//    with Configuration
    {
  import config._

  def extendedSearch(uri: String, hrefPrefix: String = hrefDisplayPrefix): Future[NodeSeq] =
    search(hrefPrefix,
        "fr", // TODO <<<<<<<<<<<<<<
        uri)

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(searchStrings: String*): String = {
    	val search = searchStrings(0)
      val q = s"""
       |# ${declarePrefix(foaf)}
       |SELECT DISTINCT ?thing WHERE {
       | graph ?g {
       |    # "backward" links distance 2
       |    ?TOPIC ?PRED <$search> .
       |    ?thing ?PRED2  ?TOPIC .
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "forward-backward" links distance 2
       |    <$search> ?PRED3 ?TOPIC2 .
       |    ?thing ?PRED4 ?TOPIC2 .
       |  }
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "forward" links distance 2
       |    <$search> ?PRED41 ?TOPIC3 .
       |    ?TOPIC3 ?PRED5 ?thing .
       |  }
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "backward-forward" links distance 2
       |    ?TOPIC4 ?PRED6 <$search> .
       |    ?TOPIC4 ?PRED7 ?thing . 
       |  }
       | }
       |}
""".stripMargin
      println("extendedSearch: query: " + q)
      q
    }
  }

}
