package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import java.net.URLEncoder
import org.w3.banana.RDFSPrefix

/** String Search with simple SPARQL or SPARQL + Lucene 
 *  (see trait LuceneIndex) */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
        with Configuration {
    
  val plainSPARQLquery = new SPARQLQueryMaker[Rdf]
        with ColsInResponse {
    override def makeQueryString(search: String): String = s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "${search.trim()}", 'i')
         |  }
         |}""".stripMargin
  }

  /** see https://jena.apache.org/documentation/query/text-query.html */
  val indexBasedQuery = new SPARQLQueryMaker[Rdf]
      with ColsInResponse {
          val rdfs = RDFSPrefix[Rdf]
    override def makeQueryString(search: String): String = s"""
         |PREFIX text: <http://jena.apache.org/text#>
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing text:query ( '${search.trim()}' 10 )
         |  }
         |}""".stripMargin
  }
  
  trait ColsInResponse extends SPARQLQueryMaker[Rdf] {
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

  private implicit def searchStringQueryMaker: SPARQLQueryMaker[Rdf] = {
		println( s"searchStringQueryMaker: useTextQuery $useTextQuery")
    if( useTextQuery )
      indexBasedQuery
    else
      plainSPARQLquery
  }

  def searchString(searchString: String, hrefPrefix: String = "",
                   lang: String = "", classURI: String = ""): Future[NodeSeq] =
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
