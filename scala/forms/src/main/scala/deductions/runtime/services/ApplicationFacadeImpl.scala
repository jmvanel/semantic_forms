package deductions.runtime.services

import java.io.{ByteArrayInputStream, OutputStream}
import java.net.URLDecoder
import java.net.URLEncoder

import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.data_cleaning.BlankNodeCleanerIncremental
import deductions.runtime.services.html.TriplesViewWithTitle
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.services.html.{CreationFormAlgo, TriplesViewWithTitle}
import deductions.runtime.sparql_cache.algos.StatisticsGraph
import deductions.runtime.sparql_cache.{BrowsableGraph, RDFCacheAlgo, SPARQLHelpers}
import deductions.runtime.user.RegisterPage
import deductions.runtime.utils.{CSSClasses, Configuration}
import deductions.runtime.core.HTTPrequest
import deductions.runtime.views.{FormHeader, Results, ToolsPage}
import deductions.runtime.utils.ServiceListenersManager

import org.w3.banana.io._
import org.w3.banana.RDF

//import play.api.libs.iteratee.Enumerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

import scalaz._
import Scalaz._
import akka.stream.scaladsl.StreamConverters
import deductions.runtime.utils.I18NMessages

/**
 * a Web Application Facade,
 *  that still exposes to client all dependences on semantic_forms implementations
 *   and Banana
 *
 * API Functions already implemented by inheritance :
 *
 *  def lookup(search: String): String
 *  def login(loginName: String, password: String): Option[String]
 *  def ldpGET(uri: String, accept: String): String = getTriples(uri, accept)
 *	def formData(uri: String, blankNode: String = "", Edit: String = "", formuri: String = ""): String
 * 
 */
trait ApplicationFacadeImpl[Rdf <: RDF, DATASET]
    extends ImplementationSettings.RDFModule
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with TriplesViewWithTitle[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CreationFormAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with StringSearchSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ReverseLinksSearchSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ExtendedSearchSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with BrowsableGraph[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormSaver[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LDP[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Lookup[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Authentication[ImplementationSettings.Rdf, ImplementationSettings.DATASET] //with ApplicationFacadeInterface
    with RegisterPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormHeader[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with TimeSeries[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with NamedGraphsSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with TriplesInGraphSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with BlankNodeCleanerIncremental[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DashboardHistoryUserActions[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with StatisticsGraph[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormJSON[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ToolsPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSSClasses
    with Results
    with ServiceListenersManager[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SFPlugins[Rdf, DATASET]
    with RecoverUtilities[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
{
 
  val config: Configuration
  import config._

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]

  import ops._

  // Members declared in org.w3.banana.JsonLDWriterModule
/*
  implicit val jsonldExpandedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdExpanded] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }
  implicit val jsonldFlattenedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdFlattened] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }
*/
  logger.debug(s"ApplicationFacadeImpl: in Global")


  /** NON transactional */
  def labelForURI(uri: String, language: String)
  (implicit graph: Rdf#Graph)
    : String = {
      makeInstanceLabel(URI(uri), graph, language)
  }

  /** Update label For URI
   *  NOTE this creates a RW transaction; do not use it too often */
  def labelForURITransaction(uri: String, language: String)
  : String = {
//    logger.info( s"labelForURITransaction $uri, $language"  )
    val tried = rdfStore.rw(dataset, {
      makeInstanceLabel(URI(uri), allNamedGraph, language)
    })
    tried match {
      case Success(s) => logger.debug( s"labelForURI <$uri> tried '$tried'" )
      case Failure(s) => logger.error( s"labelForURI <$uri> tried '$tried'" )
    }
    tried.getOrElse(uri)
  }

  /** Update label For URI
   *  NOTE this creates a R transaction, better use this */
  def labelForURITransactionFuture(uri: String, language: String) : String = {
    logger.debug( s"labelForURITransactionFuture <$uri>, $language"  )
    logger.trace(s"labelForURITransactionFuture - ${Thread.currentThread().getStackTrace().slice(0, 10).mkString("\n")}")
    makeInstanceLabelFutureTr( URI(uri), allNamedGraph, language)
  }

  def wordsearchFuture(q: String = "", clas: String = "", request: HTTPrequest): Future[Elem] = {
    val fut = recoverFromOutOfMemoryError(
      { // fillMemory() ;
        searchString(q, hrefDisplayPrefix, request, clas)
      })
    val sparqlQuery = URLEncoder.encode(queryWithlinksCountMap(q, clas), "utf-8")
    wrapSearchResults(fut, q,
      mess =
        Seq( mapButton(sparqlQuery, request),
          <span>
            { if( clas != "" )
              s"${I18NMessages.get("of_types", request.getLanguage)} <$clas> ," else "" }
            { val theme = request.getHTTPparameterValue("link").getOrElse("")
              if( theme != "" ) s", ${I18NMessages.get("linked_to", request.getLanguage)} '$theme'," else "" }
            { I18NMessages.get("search", request.getLanguage) }
          </span>
        ) )
  }

  /** Button for geographical map */
  private def mapButton(sparqlQuery: String, request: HTTPrequest): Elem =
      <a href={
          request.adjustSecure(geoMapURL) +
          "?link-prefix=" + "http://" /*TODO https case*/+ request.host + hrefDisplayPrefix +
          "&lang=" + request.getLanguage +
//          "&label=" + request.getHTTPparameterValue("label").getOrElse("") +
          "&url=" + sparqlServicesURL(request) +
          "?" + "query=" + sparqlQuery
        }
        target="_blank"
        class="sf-button_important"
        style="font-size: 1.6em;"
     > { I18NMessages.get("Map", request.getLanguage) } </a>

  /** for test, creates an OutOfMemoryError exception */
  private def fillMemory() = {
    var voidSpace = 20;
    for (outerIterator <- 1 to 50) {
      System.out.println("Iteration " + outerIterator + " Free Mem: "
        + Runtime.getRuntime().freeMemory())
      var memoryFillIntVar = new Array[Int](voidSpace)
      for (innerIterator <- 10 to 0) {
        memoryFillIntVar(innerIterator) = 0
      }
      voidSpace = voidSpace * 10
    }
  }

  def recoverFromOutOfMemoryErrorDefaultMessage(lang: String) =
    I18NMessages.get("recoverFromOutOfMemoryErrorDefaultMessage", lang)
    // "ERROR! try again some time later."
  /** recover From Out Of Memory Error, with message */
  def recoverFromOutOfMemoryError(
    sourceCode: => Future[NodeSeq],
    message:    String             =
      recoverFromOutOfMemoryErrorDefaultMessage("en") ): Future[NodeSeq] = {
    try {
      sourceCode
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        Future {
          <p>
            { message }
            <br/>
            { t.getLocalizedMessage }
          </p>
        }
    }
  }


//  def edit(url: String): NodeSeq = {
//    htmlForm(url, editable = true)._1
//  }

  /** save Form data in TDB
   *  @return main subject URI like [[FormSaver.saveTriples]],
   *  type change flag */
  def saveForm(requestMap: Map[String, Seq[String]], lang: String = "",
      userid: String, graphURI: String = "", host: String= "")
  : (Option[String], Boolean) = {
    logger.debug(s"""ApplicationFacadeImpl.saveForm: request :$requestMap,
      userid <$userid>""")
    val mainSubjectURI = try {
      implicit val userURI: String = userid
      saveTriples(requestMap,lang)
    } catch {
      case t: Throwable =>
        logger.error(s"""Exception in saveTriples: $t
            ${t.getStackTrace.slice(0,5).mkString("\n")}""")
        throw t
    }
    val uriOption = (requestMap).getOrElse("uri", Seq()).headOption
    logger.info(s"ApplicationFacadeImpl.saveForm: uriOption $uriOption, graphURI <$graphURI>")
    uriOption match {
      case Some(url1) =>
      val uri = URLDecoder.decode(url1, "utf-8")
      val res = rdfStore.rw( dataset, {
        // NOTE: purpose here: update Instance Label if rdfs:label has changed
        replaceInstanceLabel( URI(uri), allNamedGraph, lang )
    	})
    	logger.info( s"Save: normal! $uriOption" )
      case _ => logger.error(
          s"Save: focus URI not defined, request graphURI ${requestMap.getOrElse("graphURI", "?")}" )
    }
    
    // associate userid with graphURI
    rdfStore.rw( dataset, {      
    	rdfStore.appendToGraph( dataset,
              URI("urn:graphForUser"),
              makeGraph(
                  // TODO
//              Seq( Triple( URI(graphURI), URI("urn:graphForUser"), URI(userid) ) ) . toIterable
              Seq() // . toIterable
      )
      )
    })

    mainSubjectURI
  }

  /** XHTML wrapper around SPARQL Construct result TODO  move to a trait in html package */
  def sparqlConstructQueryHTML(query: String, request: HTTPrequest,
      context: Map[String,String] ): Elem = {
    logger.info(">>>> sparqlConstructQueryHTML, query\n" + query)
    <p>
      { sparqlQueryForm( true,query, "/sparql-ui",
          Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10"), request ) }
      <pre>
        {
          if( query != "" )
            sparqlConstructQueryTR(query, request: HTTPrequest, context=context)
        		  match {
                            case Success(str) =>
                              logger.debug("sparqlConstructQueryHTML, str\n" + str)
                              str
                            case Failure(f) =>
                              logger.warn("sparqlConstructQueryHTML, failure\n" + f)
                              f.getLocalizedMessage
        		  }
          /* TODO Future !!!!!!!!!!!!!!!!!!! */
        }
      </pre>
    </p>
  }

/** Display result of a SPARQL select, plus a form to edit the SPARQL text */
  def selectSPARQL(query: String, request: HTTPrequest): NodeSeq = {
    logger.debug("sparql query  " + query)
    <p>
      {
        sparqlQueryForm( false, query, "/select-ui",
          Seq("SELECT * WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10"), request)
      }
      <br></br>
      <style type="text/css">
        { cssRules }
      </style>
      {
        if (query != "") {
          val rowsTry = sparqlSelectQuery(query, context=request.queryString2 )
          rowsTry match {
            case Success(rows) =>
              val paragraphs = request.getHTTPparameterValue("paragraphs").getOrElse("")
//              println( s">>>> selectSPARQL: paragraphs '$paragraphs'")
              val results = if( paragraphs != "" )
                sentencesForSPARQLresults(rows)
              else
                tableForSPARQLresults(rows)
              loadURIsIfRequested( rows, request)
              computeLabelsIfRequested(rows, request)
              results
            case Failure(e)=> e.toString()
          }
        }
      }
    </p>
  }

  private def tableForSPARQLresults(rows: List[Iterable[Rdf#Node]]): NodeSeq = {
    <div>Result: {
      // NOTE: the first row is an empty List() !?
      rows.size - 1
    } rows</div>
    <table class="sf-sparql-table">{
      for (row <- rows) yield {
        <tr>
          { for (cell <- row) yield <td> { nodeToHTML(cell) } </td> }
        </tr>
      }
    }</table>
  }

  private def sentencesForSPARQLresults(rows: List[Iterable[Rdf#Node]]): NodeSeq = {
    <div>Result: {
      // NOTE: the first row is an empty List() !?
      rows.size - 1
    } rows</div>
    <p class="sf-values-group">{
      for (row <- rows) yield {
        <p class="sf-sentence">
          { for (cell <- row) yield <span> { cell } </span> }
        </p>
      }
    }</p>
  }

  /** backlinks: add HTML header to raw results */
  def backlinksFuture(request: HTTPrequest): Future[NodeSeq] = {
    val futureResults = backlinks(hrefDisplayPrefix, request)
    val qs = request.getQueries()
    val uri = qs(0)
    wrapSearchResults(
      futureResults, "",
      mess = <div>
        Searched for
        "<a href={ createHyperlinkString(uri = uri) }>{
          labelForURITransactionFuture(uri, request.getLanguage)
        }</a>"
        &lt;{ uri }&gt;
        {
          val sparqlQueryMap = URLEncoder.encode(reverseLinksMaps(qs), "utf-8")
          mapButton(sparqlQueryMap, request)
        }
     </div>)
  }

  /** TODO another similar function in ToolsPage 
   *  TODO maybe use request.localSparqlEndpoint */
  private def sparqlServicesURL(request: HTTPrequest) = {
//    val httpOrhttps = Map( true -> "https://", false -> "http://")
//    val servicesURIPrefix = httpOrhttps(request.secure) + request.host
    val servicesURIPrefix = request.absoluteURL()
//    println(s"servicesURIPrefix <$servicesURIPrefix>")
    val sparqlServicePrefix = "/sparql"
    val dataServicesURL = s"$servicesURIPrefix$sparqlServicePrefix"
    logger.debug(s">>>> sparqlServicesURL: dataServicesURL <$dataServicesURL>")
    dataServicesURL
  }

  def esearchFuture(q: String = "", httpRequest: HTTPrequest): Future[Elem] = {
    val fut = extendedSearch(q, httpRequest)
    wrapSearchResults(fut, q, mess= <div>"Extended search for</div>)
  }

//  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
//    slug: Option[String],
//    content: Option[String], request: HTTPrequest): Try[String] =
//    putTriples(uri, link, contentType,
//      slug, content, request)

  def makeHistoryUserActions(limit: String, request: HTTPrequest): NodeSeq =
    makeTableHistoryUserActions(request)(limit)

}
