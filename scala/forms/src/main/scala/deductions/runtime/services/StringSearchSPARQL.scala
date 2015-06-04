package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem

/** String Search with simple SPARQL */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends GenericSearchSPARQL[Rdf, DATASET] {

  def searchString(search: String): Future[Elem] = this.search(search)

  val queryMaker = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?string .
         |    FILTER regex( ?string, "$search", 'i')
         |  }
         |}""".stripMargin
  }
}