package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text

/** Show named graphs */
trait TriplesInGraphSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString( graphURI: String*): String =
    s"""
         |SELECT DISTINCT ?thing ?p ?o WHERE {
         |  graph <${graphURI(0)}> {
         |    ?thing ?p ?o .
         |  }
         |}""".stripMargin
         
    override def variables = Seq("thing", "p", "o")
    
//    override def columnsForURI( node: Rdf#Node, label: String): NodeSeq =
//      Text("test")    
  }

  def showTriplesInGraph(graphURI: String, lang: String = "") //  : Future[Elem]
  = {
//		  println(s"showTriplesInGraph: hrefDisplayPrefix ${config.hrefDisplayPrefix}")
    <p>
      <p> Triples in Graph &lt;{ graphURI }> </p>
      { search2(graphURI, config.hrefDisplayPrefix, lang) }
    </p>
  }

}