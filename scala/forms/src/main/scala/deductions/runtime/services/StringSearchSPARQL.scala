package deductions.runtime.services

import java.net.URLEncoder

import scala.concurrent.Future
import scala.xml.NodeSeq

import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix

import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.Prefix

/** String Search with simple SPARQL or SPARQL + Lucene 
 *  (see trait LuceneIndex) */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
//        with Configuration
        {
  val config: Configuration
  import config._

  val plainSPARQLquery = new SPARQLQueryMaker[Rdf]
        with ColsInResponse {
    override def makeQueryString(search: String*): String = s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, '${prepareSearchString(search(0))}', 'i')
         |  }
         |}""".stripMargin
  }

  /** see https://jena.apache.org/documentation/query/text-query.html
   *  TODO code duplicated in Lookup.scala */
  val indexBasedQuery = new SPARQLQueryMaker[Rdf] with ColsInResponse {
    override def makeQueryString(searchStrings: String*): String = {
      val search =  searchStrings(0)
      val clas = searchStrings(1)
      val limit = if( clas != "" ) "" else "LIMIT 15"

      val textQuery = if( searchStrings(0).length() > 0 )
        s"?thing text:query ( '${prepareSearchString(search).trim()}' ) ."
      else ""

      val queryString0 = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
         |  graph ?g {
         |    $textQuery
         |    ?thing ?p ?o .
         |    ?thing a ?class .
         |  }
         |}
         |GROUP BY ?thing
         |ORDER BY DESC(?count)
         |$limit""".stripMargin

      if (clas != "") {
        queryString0.replaceFirst("""\?class""", "<" + expandOrUnchanged(clas) + ">")
      } else queryString0
    }
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
    search(hrefPrefix, lang, searchString, classURI)

}
