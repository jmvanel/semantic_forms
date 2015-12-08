package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem

/** Show named graphs */
trait RDFDashboardSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
        // TODO show # of triples
    s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?thing {
         |    [] ?p ?O .
         |  }
         |}""".stripMargin
  }

  def showNamedGraphs( lang: String = ""): Future[Elem] =
    search("", "", lang)

}