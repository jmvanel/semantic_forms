package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem

/** Broader "interest "  search in simple search results page */
trait ExtendedSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  def extendedSearch(uri: String, hrefPrefix: String = ""): Future[Elem] =
    search(uri, hrefPrefix)

  private implicit val queryMaker = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
      s"""
        prefix foaf: <http://xmlns.com/foaf/0.1/>
        
SELECT DISTINCT ?PERSON WHERE {
{
?TOPIC ?PRED  <$search> .
?PERSON  ?PRED2  ?TOPIC .
?PERSON a foaf:Person .
} UNION {
  <$search> ?PRED3 ?TOPIC2 .
?PERSON  ?PRED4  ?TOPIC2 .
?PERSON a foaf:Person .
}
}
"""

  }

}
