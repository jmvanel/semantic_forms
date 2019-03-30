package deductions.runtime.services

import deductions.runtime.abstract_syntax.{InstanceLabelsInferenceMemory, PreferredLanguageLiteral}
import deductions.runtime.html.Form2HTMLDisplay
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.views.ResultsDisplay
import org.w3.banana.{RDF, TryW}

import scala.concurrent.Future
import scala.xml.{Elem, NodeSeq, Text}
import scala.xml.Unparsed
import deductions.runtime.core.HTTPrequest

import scalaz._
import Scalaz._
import deductions.runtime.sparql_cache.RDFCacheAlgo
import java.net.URLEncoder

trait SPARQLQueryMaker[Rdf <: RDF] {
  // TODO : search: String*
  def makeQueryString(search: String*): String
  def variables = Seq("thing")
    /** overridable function for adding columns in response */
  def columnsForURI( node: Rdf#Node, label: String): NodeSeq = Text("")

  /** prepare Search String: trim, and replace ' with \' */
  def prepareSearchString(search: String) = {
    search.trim().replace("'", """\'""")
  }
}

/**
 * Generic SPARQL SELECT Search with single parameter,
 * and single return URI value,
 *  and showing in HTML a column of hyperlinked results with instance Labels
 */
abstract trait ParameterizedSPARQL[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with Form2HTMLDisplay[Rdf#Node, Rdf#URI]
    with ResultsDisplay[Rdf, DATASET]
    with RDFCacheAlgo[Rdf, DATASET] {

  import config._

  /**
   * Generic SPARQL SELECT with single result columns (must be named "thing"),
   * generate a column of HTML hyperlinks for given search string;
   * search and display results as an XHTML element
   * read transactional
   * @param hrefPrefix URL prefix for creating hyperlinks ((URI of each query result is concatenated)
   *
   * TODO
   * - lang is already part of httpRequest
   * - displayResults should be a function argument
   * - search2 is very similar!
   */
  def search(
    hrefPrefix:  String,
    lang:        String,
    search:      Seq[String],
    variables:   Seq[String] = Seq("?thing"),
    httpRequest: HTTPrequest = HTTPrequest())(
    implicit
    queryMaker: SPARQLQueryMaker[Rdf]): Future[NodeSeq] = {
    logger.debug(s"search($search) 1: starting TRANSACTION for dataset $dataset")
    val elem0 = rdfStore.r(dataset, {
      val uris = search_onlyNT(search, variables, httpRequest)
      logger.info(s"search: ${httpRequest.logRequest()}: URI's size ${uris.size}")
      loadURIsIfRequested( uris, httpRequest)
      val graph: Rdf#Graph = allNamedGraph
      val sparqlQuery = queryMaker.makeQueryString(search(0))
      val elems = Seq(
        <button value="Sort" id="sort"> Sort </button>,
        sortJSscript,
        sparqlQueryButton(sparqlQuery),
        showContinuationForm( httpRequest ),
        <div id="container" class={ cssConfig.formRootCSSClass }> {
          localCSS ++
            uris.map {
              u =>
                // logger.trace(s"\tsearch(): URI row $u")
                displayResults(u, hrefPrefix, lang, graph, false, httpRequest)
            }
        }</div>)
      elems
    })
    logger.debug(s"search: leaving TRANSACTION for dataset $dataset")
    val elem = elem0.get
    Future.successful(elem)
  }

  /** Edit Select SPARQL query button */
  def sparqlQueryButton(sparqlQuery: String) =
    <span class="sf-expert">
      <a href={
        "/select-ui?query=" +
          URLEncoder.encode(sparqlQuery, "utf-8")
      }>Edit SPARQL query</a>
      &nbsp;
    </span>

  import scala.concurrent.ExecutionContext.Implicits.global
  /**
   * load URI's If Requested, that is when HTTP parameter "load-uris" is Defined
   * in a Future
   */
  private def loadURIsIfRequested(
    uris:        List[Seq[Rdf#Node]],
    httpRequest: HTTPrequest) =
    if (httpRequest.getHTTPparameterValue("load-uris").isDefined)
      for (uriRow <- uris)
        Future {
          val uri = uriRow.headOption.getOrElse(nullURI)
          retrieveURIBody(
            forceNodeToURI(uri), dataset,
            httpRequest, transactionsInside = true)
          logger.info(s"loadURIsIfRequested: $uri was loaded.")
        }

  private val sortJSscript =
    <script> {
      Unparsed("""
        //// console.log("divs " + JSON.stringify($divs) );
        $('#sort').on('click', function () {
              var $divs = $("div.sf-triple-block")
              $divs.sort(function (a, b) {
                  var atext = $(a).find(".sf-rdf-link").text().trim().toLowerCase().replace("_", " ")
                  var btext = $(b).find(".sf-rdf-link").text().trim().toLowerCase().replace("_", " ")
                  return atext.localeCompare(btext)
              })
//            for (e of $divs) {
//                console.log( '"' + $(e).find(".sf-rdf-link").text().trim().toLowerCase().replace("_", " ") + '"' )
//              }
              let wrapper = $('#container')
              wrapper.empty()
              for (e of $divs) {
                $(e).appendTo(wrapper)
              }
            })
      """)
    } </script>

  /**
   * Generic SPARQL SELECT Search with multiple result columns;
   * search and display results as an XHTML element;
   * generate rows of HTML hyperlinks for given search string;
   * transactional
   * @param hrefPrefix URL prefix for creating hyperlinks ((URI of each query result is concatenated)
   */
  def search2(search: String, hrefPrefix: String = config.hrefDisplayPrefix,
              lang: String = "")(implicit queryMaker: SPARQLQueryMaker[Rdf]): Elem = {
    val uris = search_only2(search)
    val elem0 =
      rdfStore.rw(dataset, {
        val graph: Rdf#Graph = allNamedGraph
        <div class={ cssConfig.formRootCSSClass }>
        <div>Size: { uris.size }</div>
        {
          localCSS ++
            uris.map {
              // create table like HTML
              u =>
                // logger.trace(s"**** search2 u $u")
                displayResults(u.toIterable, hrefPrefix, lang, graph)
            }
        }</div>
      })
    val elem = elem0.get
    elem
  }

  /**
   * TRANSACTIONAL
   *
   * CAUTION: It is of particular importance to note that
   * one should never use an Iterator after calling a method on it;
   * cf http://stackoverflow.com/questions/18420995/scala-iterator-one-should-never-use-an-iterator-after-calling-a-method-on-it
   */
  private def search_only(search: String*)(implicit queryMaker: SPARQLQueryMaker[Rdf]): 
	  Future[List[Seq[Rdf#Node]]]
			  // : Future[Iterator[Rdf#Node]]
		= {
    logger.debug(s"search 2: starting TRANSACTION for dataset $dataset")
    val transaction =
      rdfStore.r(dataset, {
        search_onlyNT(search)
      })
    val tryIteratorRdfNode = transaction // .flatMap { identity }
    logger.debug(s"after search_only(search tryIteratorRdfNode $tryIteratorRdfNode")
    tryIteratorRdfNode.asFuture
  }
  
  /** search only Non Transactional; need READ transaction */
  private def search_onlyNT(search: Seq[String], variables: Seq[String] = Seq("?thing"),
      httpRequest: HTTPrequest = HTTPrequest())
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ) = {
    val queryString: String = {
      val rawQueryString = queryMaker.makeQueryString(search :_* )
      addLimit(rawQueryString, httpRequest)
    }
    logger.debug(
      s"search_onlyNT(search='$search') \n$queryString \n\tdataset Class ${dataset.getClass().getName}" )
    // NOTE: if class is specified in request, then ?CLASS is not in results, and vice-versa
    logger.debug( s"search_onlyNT: search: ($search) : SPARQL variables $variables" )
    sparqlSelectQueryVariablesNT(queryString, variables )
  }

  /** with result variables specified; transactional */
  private def search_only2(search: String)
  (implicit queryMaker: SPARQLQueryMaker[Rdf] ): List[Seq[Rdf#Node]] = {
    val dsg = dataset.asInstanceOf[org.apache.jena.sparql.core.DatasetImpl].asDatasetGraph()
    logger.debug(s">>>> dsg class : ${dsg.getClass}")
    
    val queryString = queryMaker.makeQueryString(search)
	  logger.debug( s"search_only2( search $search" )
    sparqlSelectQueryVariables(queryString, queryMaker.variables )
  }

  private def addLimit(rawQueryString: String, httpRequest: HTTPrequest) = {
    val limitOption = httpRequest.getHTTPparameterValue("limit")
    val queryWithLimit = limitOption match {
      case Some(limit) if (limit =/= "") => s"$rawQueryString LIMIT $limit"
      case Some(_)                       => s"$rawQueryString LIMIT $defaultSPARQLlimit"
      case None                          => s"$rawQueryString LIMIT $defaultSPARQLlimit"
    }
    val offsetOption = httpRequest.getHTTPparameterValue("offset")
    offsetOption match {
      case Some(offset)  if( offset =/= "" ) => s"$queryWithLimit OFFSET $offset"
      case Some(_) => rawQueryString
      case None => queryWithLimit
    }
  }

}
