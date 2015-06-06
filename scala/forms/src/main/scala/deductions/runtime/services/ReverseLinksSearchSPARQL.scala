package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem

/** Reverse Links Search with simple SPARQL */
trait ReverseLinksSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  def backlinks(uri: String, hrefPrefix: String = ""): Future[Elem] =
    search(uri, hrefPrefix)

  private implicit val queryMaker = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p <$search> .
         |  }
         |}""".stripMargin
  }

}