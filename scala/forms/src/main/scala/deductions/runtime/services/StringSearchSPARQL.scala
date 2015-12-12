package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import java.net.URLEncoder

/** String Search with simple SPARQL */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String): String = s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "${search.trim()}", 'i')
         |  }
         |}""".stripMargin

    /** add columns in response */
    override def columnsForURI(node: Rdf#Node, label: String): NodeSeq = {
      <a href={
        "/display?displayuri=" +
          URLEncoder.encode(node.toString(), "UTF-8")
      } class="form-value">
        Show Triples in local database
      </a>
    }
  }

  def searchString(searchString: String, hrefPrefix: String = "",
                   lang: String = ""): Future[NodeSeq] =
    search(searchString, hrefPrefix, lang)
    
//  private val searchStringQueryMaker2 = new SPARQLQueryMaker {
//    override def makeQueryString(search: String): String =
//      s"""
//         |SELECT DISTINCT ?thing WHERE {
//         |  {
//         |  graph ?g {
//         |    ?thing ?p ?o .
//         |    FILTER regex( ?o, "$search", 'i')
//         |  }
//         |  } UNION {
//         |  graph ?g0 {
//         |    ?thing ?p ?o .
//         |  }
//         |  graph ?g1 {
//         |   ?o ?p2 ?o2 .
//         |    FILTER regex( ?o2, "$search", 'i')
//         |  }
//         |  }
//         |}""".stripMargin
//  }
}