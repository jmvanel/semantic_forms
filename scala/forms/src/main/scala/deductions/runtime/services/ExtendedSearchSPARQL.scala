package deductions.runtime.services

import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Broader "interest" search in simple search results page
 *  TESTED with
 *  http://localhost:9000/esearch?q=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
 */
trait ExtendedSearchSPARQL[Rdf <: RDF, DATASET]
  extends ParameterizedSPARQL[Rdf, DATASET]
  with RDFPrefixes[Rdf]
  with NavigationSPARQLBase[Rdf] {

  import config._

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(searchStrings: String*): String = {
      val search = searchStrings(0)
      val sparqLQuery = extendedSearchSPARQL(search)
      logger.debug(s"extendedSearch: query: $sparqLQuery")
      sparqLQuery
    }
  }

  def extendedSearch(uri: String, hrefPrefix: String = hrefDisplayPrefix): Future[NodeSeq] =
    search(
      hrefPrefix,
      "fr", // TODO <<<<<<<<<<<<<<
      Seq(uri))

}
