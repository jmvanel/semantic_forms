package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import java.net.URLEncoder

/** Show named graphs */
trait RDFDashboardSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String): String =
      // TODO show # of triples
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?thing {
         |    [] ?p ?O .
         |  }
         |}""".stripMargin

    /** add columns in response */
    override def columnsForURI(node: Rdf#Node, label: String): NodeSeq = {
//      println("RDFDashboardSPARQL.columnsForURI")
      <a href={
        "/showTriplesInGraph?uri=" +
          URLEncoder.encode(node.toString(), "UTF-8")
      } class="form-value">
        Show Triples in graph
      </a>
    }
  }
  
  def showNamedGraphs( lang: String = ""): Future[NodeSeq] =
//    Future.successful( 
		  search("", "", lang)
    
}