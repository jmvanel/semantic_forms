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
    with ToolsPage
    with CSSClasses
    with Results
    with ServiceListenersManager[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SFPlugins[Rdf, DATASET] {
 
  val config: Configuration
  import config._

//  addSaveListener(this) // for TimeSeries
//  addSaveListener( new RDFLinksCounterListenerClass(config) )
//  addServiceListener(new RDFLinksCounterLoadListenerClass(config, this) )

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]

  import ops._

  // Members declared in org.w3.banana.JsonLDWriterModule
  implicit val jsonldExpandedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdExpanded] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }
  implicit val jsonldFlattenedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdFlattened] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }

  logger.info(s"in Global")



  /** NON transactional */
  def labelForURI(uri: String, language: String)
  (implicit graph: Rdf#Graph)
    : String = {
      makeInstanceLabel(URI(uri), graph, language)
  }
  /** NOTE this creates a transaction; do not use it too often */
  def labelForURITransaction(uri: String, language: String)
  : String = {
//    logger.info( s"labelForURITransaction $uri, $language"  )
    val res = rdfStore.rw(dataset, {
      makeInstanceLabel(URI(uri), allNamedGraph, language)
    }).getOrElse(uri)
//    logger.info( s"result $res"  )
    res
  }

  //    def displayURI2(uriSubject: String) //  : Enumerator[scala.xml.Elem]
  //    = {
  //      import ops._
  //      val graphFuture = RDFStoreObject.allNamedGraphsFuture
  //      import scala.concurrent.ExecutionContext.Implicits.global
  //
  //      type URIPair = (Rdf#Node, SemanticURIGuesser.SemanticURIType)
  //      val semanticURItypesFuture = tableView.getSemanticURItypes(uriSubject)
  //      // TODO get rid of mutable, but did not found out with yield
  //      val elems: Future[Iterator[Elem]] = semanticURItypesFuture map {
  //        semanticURItypes =>
  //          {
  //            semanticURItypes.
  //              filter { p => isURI(p._1) }.
  //              map {
  //                semanticURItype =>
  //                  val uri = semanticURItype._1
  //                  val semanticType = semanticURItype._2
  //                  <p>
  //                    <div>{ uri }</div>
  //                    <div>{ semanticType }</div>
  //                  </p>
  //              }
  //          }
  //      }
  //      //    def makeEnumerator[E, A]( f: Future[Iterator[A]] ) : Enumerator[A] = new Enumerator[A] {
  //      //      def apply[A]( i : Iteratee[A, Iterator[A]]): Future[Iteratee[A, Iterator[A]]]
  //      //      = {
  //      //        Future(i) // ?????
  //      //      }
  //      //    }
  //      //    val enum = makeEnumerator(elems) // [ , ]
  //      elems
  //    }

  def wordsearchFuture(q: String = "", clas: String = "", request: HTTPrequest): Future[Elem] = {
    val fut = recoverFromOutOfMemoryError(
      { // fillMemory() ;
        searchString(q, hrefDisplayPrefix, request, clas)
      })
    val sparqlQuery = URLEncoder.encode(queryWithlinksCountMap(q), "utf-8")
    wrapSearchResults(fut, q,
      mess =
        Seq( mapButton(sparqlQuery, request),
             <span>In class &lt;{ clas }&gt;, searched for</span>
        ) )
  }

  /** Button for geographical map */
  private def mapButton(sparqlQuery: String, request: HTTPrequest): Elem =
      <a href={
          request.adjustSecure(geoMapURL) +
          "?link-prefix=" + "http://" /*TODO https case*/+ request.host + hrefDisplayPrefix +
          "&lang=" + request.getLanguage() +
          "&url=" + sparqlServicesURL(request) +
          "?" + "query=" + sparqlQuery
        }
        target="_blank"
     > Map </a>

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

  def recoverFromOutOfMemoryError(
    sourceCode: => Future[NodeSeq],
    message:    String             = "ERROR! try again some time later."): Future[NodeSeq] = {
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


  /** Getting the runtime reference from system */
  private val runtime = Runtime.getRuntime

  def recoverFromOutOfMemoryErrorGeneric[T](
    sourceCode: => T,
    error: Throwable => T ): T = {
   val freeMemory = runtime.freeMemory()
   if( freeMemory < 1024 * 1024 * 10)
     error(new OutOfMemoryError("Free Memory < 10 mb, retry later."))
   else
    try {
      sourceCode
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        printMemory()
        error(t)
    }
  }

  def downloadAsString(url: String, mime: String="text/turtle"): String = {
    logger.info( s"download url $url mime $mime")
    val res = focusOnURI(url, mime)
    logger.info(s"""download result "$res" """)
    res
  }

  /** implements download of RDF content from HTTP client;
   *  TODO should be non-blocking !!!!!!!!!!!!!
   *  currently accumulates a string first !!!
   *  not sure if Banana and Jena allow a non-blocking access to SPARQL query results */
  def download(url: String, mime: String="text/turtle") = {
	  val res = downloadAsString(url, mime)
	  val input = new ByteArrayInputStream(res.getBytes("utf-8"))
	  StreamConverters.fromInputStream(() ⇒ input)
  }

  /** not working !!!!!!!!!!!!!  */
//  private def downloadKO(url: String): Enumerator[Array[Byte]] = {
//    // cf https://www.playframework.com/documentation/2.3.x/ScalaStream
//    // and http://greweb.me/2012/11/play-framework-enumerator-outputstream/
//    Enumerator.outputStream { os =>
//      val graph = search_only(url)
//      logger.info(s"after search_only($url)")
//      val r = graph.map { graph =>
//        /* non blocking */
//        val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
//        logger.info("before writer.write()")
//        val ret = writer.write(graph, os, base = url)
//        logger.info("after writer.write()")
//        os.close()
//      }
//      logger.info("after graph.map()")
//    }
//  }

//  def edit(url: String): NodeSeq = {
//    htmlForm(url, editable = true)._1
//  }

  /** save Form data in TDB
   *  @return main subject URI like [[FormSaver.saveTriples]],
   *  type change flag */
  def saveForm(request: Map[String, Seq[String]], lang: String = "",
      userid: String, graphURI: String = "", host: String= "")
  : (Option[String], Boolean) = {
    logger.info(s"ApplicationFacadeImpl.saveForm: request :$request, userid <$userid>")
    val mainSubjectURI = try {
      implicit val userURI: String = userid
      saveTriples(request,lang)
    } catch {
      case t: Throwable =>
        logger.error(s"""Exception in saveTriples: $t
            ${t.getStackTrace.slice(0,5).mkString("\n")}""")
        throw t
    }
    val uriOption = (request).getOrElse("uri", Seq()).headOption
    logger.info(s"ApplicationFacadeImpl.saveForm: uriOption $uriOption, graphURI $graphURI")
    uriOption match {
      case Some(url1) =>
      val uri = URLDecoder.decode(url1, "utf-8")
      val res = rdfStore.rw( dataset, {
        // NOTE: purpose here: update Instance Label if rdfs:label has changed
        replaceInstanceLabel( URI(uri), allNamedGraph, // TODO reuse allNamedGraph
            lang )
    	})
    	logger.info( s"Save: normal! $uriOption" )
      case _ => logger.error( s"Save:  NOT normal! uriOption: $uriOption request $request" )
    }
    
    // associate userid with graphURI
    rdfStore.rw( dataset, {      
    	rdfStore.appendToGraph( dataset,
              URI("urn:graphForUser"),
              makeGraph(
                  // TODO
//              Seq( Triple( URI(graphURI), URI("urn:graphForUser"), URI(userid) ) ) . toIterable
              Seq() . toIterable
      )
      )
    })

    mainSubjectURI
  }

  /** XHTML wrapper around SPARQL Construct result TODO  move to a trait in html package */
  def sparqlConstructQueryHTML(query: String, lang: String = "en", request: HTTPrequest): Elem = {
    logger.info("Global.sparql query  " + query)
    <p>
		{ sparqlQueryForm( true,query, "/sparql-ui",
				Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10"), request ) }
      <pre>
        {
//          try {
        	  if( query != "" )
        		  sparqlConstructQueryTR(query)
        		  match {
        		    case Success(str) => str
        		    case Failure(f) => f.getLocalizedMessage
        		  }
//          } catch {
//            case NonFatal(e) => e.printStackTrace() // TODO: handle error?
//          }
          /* TODO Future !!!!!!!!!!!!!!!!!!! */
        }
      </pre>
    </p>
  }

  /** SPARQL result
   *
   * @param format : "turtle" or "rdfxml" or "jsonld"
   */
  def sparqlConstructResult(query: String, lang: String,
      format: String = "turtle",
      context: Map[String,String] = Map()): String = {
    logger.info("Global.sparql query  " + query)
    if (query != "")
      sparqlConstructQueryTR(query, format,
          context + ("lang" -> lang))
      // TODO this code is in several places !!!!!! make function printTry( tr: Try[_] , mime:String)
      match {
        case Success(s) => s
        case Failure(f) =>
          if( format === "turtle")
          s"# $f" else "{error: \"" + f + "\" }"
      }
    else "# Empty query result !!!"
  }

/** Display result of a SPARQL select, plus a form to edit the SPARQL text */
  def selectSPARQL(query: String, lang: String = "en", request: HTTPrequest): Elem = {
    logger.info("sparql query  " + query)
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
              <div>Result: {rows.size} rows</div>

              <table class="sf-sparql-table">{
                val printedRows = for (row <- rows) yield {
                  <tr>
                    { for (cell <- row) yield <td> { cell } </td> }
                  </tr>
                }
                printedRows
              }</table>
            case Failure(e)=> e.toString()
          }
        }
      }
    </p>
  }

  def backlinksFuture(query: String = "", request: HTTPrequest): Future[NodeSeq] = {
    val futureResults = backlinks(query, hrefDisplayPrefix, request)
    val label = labelForURITransaction(query, language = request.getLanguage())
    val sparqlQuery = URLEncoder.encode(reverseLinksMaps(query), "utf-8")
    val messOld =
      <div>
        Searched for
        "<a href={ createHyperlinkString(uri = query) }>{ label }</a>
        "
        &lt;{ query }
        &gt;
        <div><a href={
          request.adjustSecure(geoMapURL) + "?url=" + sparqlServicesURL(request) +
            "?" +
            "query=" + sparqlQuery
        } target="_blank"> Map </a></div>
      </div>
    wrapSearchResults(futureResults, "",
      mess =
        <div>
          Searched for
          "<a href={ createHyperlinkString(uri = query) }>{ label }</a>"
          &lt;{ query }&gt;
          { mapButton(sparqlQuery, request) }
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
    println(s">>>> dataServicesURL <$dataServicesURL>")
    dataServicesURL
  }
 
  def esearchFuture(q: String = ""): Future[Elem] = {
    val fut = extendedSearch(q)
    wrapSearchResults(fut, q, mess= <div>"Extended search for</div>)
  }

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] =
    putTriples(uri, link, contentType,
      slug, content, request)

  def makeHistoryUserActions(limit: String, request: HTTPrequest): NodeSeq =
    makeTableHistoryUserActions(request)(limit)

  def formatMemory(): String = {
    val mb = 1024 * 1024
    "\n##### Heap utilization statistics [MB] #####\n" +
    "Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb +
    "\nFree Memory:" + runtime.freeMemory() / mb +
    //Print total available memory
    "\nTotal Memory:" + runtime.totalMemory() / mb +
    "\nMax Memory:" + runtime.maxMemory() / mb + "\n"
  }

  def printMemory() = logger.error( formatMemory() )
}
