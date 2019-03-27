package deductions.runtime.services

import java.net.URLEncoder

import deductions.runtime.utils.{Configuration, RDFPrefixes}
import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest

/** String Search with simple SPARQL or SPARQL + Lucene,
 *  depending on config. item useTextQuery
 *  (see trait LuceneIndex)
 */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with StringSearchSPARQLBase[Rdf] {

  val config: Configuration
  import config._

  /** index Based or not, depending on config. item useTextQuery */
  private val indexBasedQuery = new SPARQLQueryMaker[Rdf] with ColsInResponse {
    override def makeQueryString(searchStrings: String*): String = {
//      println( s"makeQueryString ${searchStrings.mkString(", ")}")
      val search =  searchStrings.headOption.getOrElse("")
      val clas = if(searchStrings.size >= 2 ) searchStrings(1) else ""

      // TODO add argument "limit" to queryWithlinksCount()
      val limit = if( clas != "" ) "" else "LIMIT 15"

      queryWithlinksCount( search, clas )
    }

  }
  
  trait ColsInResponse extends SPARQLQueryMaker[Rdf] {
    /** add columns in response - NOTE: NEVER CALLED !!! */
    override def columnsForURI(node: Rdf#Node, label: String): NodeSeq = {
      <a href={
        hrefDisplayPrefix +
          URLEncoder.encode(node.toString(), "UTF-8")
      } class="form-value">
        Show Triples in local database
      </a>
    }
  }

  private implicit def searchStringQueryMaker: SPARQLQueryMaker[Rdf] = {
		logger.debug( s"searchStringQueryMaker: useTextQuery $useTextQuery")
    indexBasedQuery
  }

  def searchString(searchString: String, hrefPrefix: String = config.hrefDisplayPrefix,
                   request: HTTPrequest=HTTPrequest(), classURI: String = ""
                   ): Future[NodeSeq] = {
		logger.debug( s"searchString: SPARQL ${indexBasedQuery.makeQueryString(searchString, classURI)}")
    search(hrefPrefix, request.getLanguage(),
        Seq(searchString, classURI),
//        Seq("?thing", "?CLASS"))
        Seq("?thing"),
        httpRequest=request)
}

}
