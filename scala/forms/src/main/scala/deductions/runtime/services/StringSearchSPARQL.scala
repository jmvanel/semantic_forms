package deductions.runtime.services

import java.net.URLEncoder

import deductions.runtime.utils.{Configuration, RDFPrefixes}
import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq

/** String Search with simple SPARQL or SPARQL + Lucene,
 *  depending on config. item useTextQuery
 *  (see trait LuceneIndex)
 * TODO common code with Lookup
 */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with StringSearchSPARQLBase[Rdf] {

  val config: Configuration
  import config._

  private val plainSPARQLquery = new SPARQLQueryMaker[Rdf]
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
  private val indexBasedQuery = new SPARQLQueryMaker[Rdf] with ColsInResponse {
    override def makeQueryString(searchStrings: String*): String = {
      val search =  searchStrings(0)
      val clas = searchStrings(1)
      val limit = if( clas != "" ) "" else "LIMIT 15"

      val textQuery = if( search.length() > 0 )
        s"?thing text:query ( '${prepareSearchString(search).trim()}' ) ."
      else ""

      // TODO val classQuery = if( clas != "") { // like textQuery before

      val queryString0 = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
         |  graph ?g {
         |    $textQuery
         |    ?thing ?p ?o .
         |    # ?thing a ?class .
         |  }
         |}
         |GROUP BY ?thing
         |ORDER BY DESC(?count)
         |$limit""".stripMargin

      // TODO val classQuery
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

  def searchString(searchString: String, hrefPrefix: String = config.hrefDisplayPrefix,
                   lang: String = "", classURI: String = ""): Future[NodeSeq] =
    search(hrefPrefix, lang, searchString, classURI)

}
