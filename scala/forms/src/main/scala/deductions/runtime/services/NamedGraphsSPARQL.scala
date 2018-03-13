package deductions.runtime.services

import java.net.URLEncoder

import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest

/** Show named graphs */
trait NamedGraphsSPARQL[Rdf <: RDF, DATASET]
    extends NavigationSPARQLBase[Rdf]
    with ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = {
      val searchArg = if( search.size > 0 ) {
        Some(search(0))
      }
      else None
      namedGraphs(searchArg)
    }

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
  
  def showNamedGraphs(httpRequest: HTTPrequest): Future[NodeSeq] = {
    val lang = httpRequest.getLanguage()
    val patternOption = httpRequest.getHTTPparameterValue("pattern")
    val searchArg = patternOption  match {
      case Some(patt) => Seq(patt)
      case None => Seq()
    }
    search("/showTriplesInGraph?uri=", lang, searchArg,
        httpRequest=httpRequest)
  }

}
