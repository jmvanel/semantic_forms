package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

/**
 * Broader "interest" search in simple search results page
 *  TESTED with
 *  http://localhost:9000/esearch?q=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
 */
trait ExtendedSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  def extendedSearch(uri: String, hrefPrefix: String = ""): Future[NodeSeq] =
    search(uri, hrefPrefix)

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String): String = {
      val q = s"""
        prefix foaf: <http://xmlns.com/foaf/0.1/>
        
SELECT DISTINCT ?S WHERE {
{
  graph ?g {
  ?TOPIC ?PRED <$search> .
  ?S ?PRED2  ?TOPIC .
  # ?S a foaf:Person .
} OPTIONAL {
  <$search> ?PRED3 ?TOPIC2 .
  ?S  ?PRED4  ?TOPIC2 .
  # ?S a foaf:Person .
}
}
}
"""
      println("extendedSearch: query: " + q)
      q
    }
  }

}
