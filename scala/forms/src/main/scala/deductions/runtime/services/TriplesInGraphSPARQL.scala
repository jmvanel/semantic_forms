package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text

/** Show named graphs */
trait TriplesInGraphSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker {
    override def makeQueryString( graphURI: String): String =
    s"""
         |SELECT DISTINCT ?thing ?p ?o WHERE {
         |  graph <$graphURI> {
         |    ?thing ?p ?o .
         |  }
         |}""".stripMargin
         
    override def variables = Seq("thing", "p", "o")
  }

  def showTriplesInGraph( graphURI: String, lang: String = "")
//  : Future[Elem]
  = search2( graphURI, "", lang)

  override def columnsForURI( node: Rdf#Node, label: String): NodeSeq = Text("")

}