package deductions.runtime.services

import java.net.URLDecoder
import java.net.URLEncoder
import java.io.ByteArrayInputStream
import java.io.OutputStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Text

import org.w3.banana.RDF
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import org.w3.banana.io.JsonLdCompacted
import org.w3.banana.io.JsonLdExpanded
import org.w3.banana.io.JsonLdFlattened

import play.api.libs.iteratee.Enumerator

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.CreationFormAlgo
import deductions.runtime.html.TriplesViewModule
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.I18NMessages
import deductions.runtime.views.FormHeader
import deductions.runtime.views.ToolsPage
import deductions.runtime.user.RegisterPage
import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.semlogs.LogAPI
import deductions.runtime.utils.CSSClasses
import deductions.runtime.data_cleaning.BlankNodeCleanerIncremental
import scala.util.control.NonFatal
import scala.util.control.NonFatal
import deductions.runtime.sparql_cache.algos.StatisticsGraph
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.views.Results
import deductions.runtime.html.TriplesViewWithTitle

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
    extends RDFCacheAlgo[Rdf, DATASET]
    with TriplesViewWithTitle[Rdf, DATASET]
    with CreationFormAlgo[Rdf, DATASET]
    with StringSearchSPARQL[Rdf, DATASET]
    with ReverseLinksSearchSPARQL[Rdf, DATASET]
    with ExtendedSearchSPARQL[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with FormSaver[Rdf, DATASET]
    with LDP[Rdf, DATASET]
    with Lookup[Rdf, DATASET]
    with Authentication[Rdf, DATASET] //with ApplicationFacadeInterface
    with RegisterPage[Rdf, DATASET]
    with FormHeader[Rdf, DATASET]
    with TimeSeries[Rdf, DATASET]
    with NamedGraphsSPARQL[Rdf, DATASET]
    with TriplesInGraphSPARQL[Rdf, DATASET]
    with BlankNodeCleanerIncremental[Rdf, DATASET]
    with DashboardHistoryUserActions[Rdf, DATASET]
    with StatisticsGraph[Rdf]
    with FormJSON[Rdf, DATASET]
    with ToolsPage
    with CSSClasses
    with Results
    {
 
  val config: Configuration
  import config._

  addSaveListener(this) // for TimeSeries

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

  import rdfStore.transactorSyntax._



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
    val res = rdfStore.r(dataset, {
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

  def wordsearchFuture(q: String = "", lang: String = "", clas: String = ""): Future[Elem] = {
    val fut = searchString(q, hrefDisplayPrefix, lang, clas)
    wrapSearchResults(fut, q, mess= s"In class <$clas>, searched for" )
  }

  def rdfDashboardFuture(q: String = "", lang: String = ""): Future[NodeSeq] = {
    val fut = showNamedGraphs(lang)
    wrapSearchResults(fut, q)
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
  def download(url: String, mime: String="text/turtle"): Enumerator[Array[Byte]] = {
	  val res = downloadAsString(url, mime)
	  val input = new ByteArrayInputStream(res.getBytes("utf-8"))
	  Enumerator.fromStream(input, chunkSize=256*256)
  }

  /** TODO not working !!!!!!!!!!!!!  */
  def downloadKO(url: String): Enumerator[Array[Byte]] = {
    // cf https://www.playframework.com/documentation/2.3.x/ScalaStream
    // and http://greweb.me/2012/11/play-framework-enumerator-outputstream/
    Enumerator.outputStream { os =>
      val graph = search_only(url)
      logger.info(s"after search_only($url)")
      val r = graph.map { graph =>
        /* non blocking */
        val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
        logger.info("before writer.write()")
        val ret = writer.write(graph, os, base = url)
        logger.info("after writer.write()")
        os.close()
      }
      logger.info("after graph.map()")
    }
  }

  def edit(url: String): NodeSeq = {
    htmlForm(url, editable = true)._1
  }

  /** save Form data in TDB
   *  @return main subject URI like [[FormSaver.saveTriples]],
   *  type change flag */
  def saveForm(request: Map[String, Seq[String]], lang: String = "",
      userid: String, graphURI: String = "", host: String= "")
  : (Option[String], Boolean) = {
    logger.info(s"ApplicationFacadeImpl.saveForm: request :$request, userid <$userid>")
    val mainSubjectURI = try {
      implicit val userURI: String = userid
      saveTriples(request)
    } catch {
      case t: Throwable =>
        logger.error("Exception in saveTriples: " + t)
        throw t
    }
    val uriOption = (request).getOrElse("uri", Seq()).headOption
    logger.info(s"ApplicationFacadeImpl.saveForm: uriOption $uriOption, graphURI $graphURI")
    uriOption match {
      case Some(url1) =>
      val uri = URLDecoder.decode(url1, "utf-8")
      val res = rdfStore.rw( dataset, {
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

  /** XHTML wrapper around SPARQL Construct result */
  def sparqlConstructQuery(query: String, lang: String = "en"): Elem = {
    logger.info("Global.sparql query  " + query)
    <p>
		{ sparqlQueryForm(true,query, "/sparql-ui",
				Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10") ) }
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

  /**
   * SPARQL result
   *  @param format = "turtle" or "rdfxml" or "jsonld"
   *  
   *  TODO move to non-Play! project
   */
  def sparqlConstructResult(query: String, lang: String = "en", format: String = "turtle"): String = {
    logger.info("Global.sparql query  " + query)
    if (query != "")
      sparqlConstructQueryTR(query, format)
      // TODO this code is in several places !!!!!! make function printTry( tr: Try[_] , mime:String)
      match {
        case Success(s) => s
        case Failure(f) =>
          if( format == "turtle")
          s"# $f" else "{error: \"" + f + "\" }"
      }
    else "# Empty query result !!!"
  }

  /** Display result of a SPARQL select */
  def selectSPARQL(query: String, lang: String = "en"): Elem = {
    logger.info("sparql query  " + query)
    <p>
      {
        sparqlQueryForm(false, query, "/select-ui",
          Seq("SELECT * WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10"))
      }
      <br></br>
      <style type="text/css">
        { cssRules }
      </style>
      {
        if (query != "") {
          val rowsTry = sparqlSelectQuery(query)
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

  def backlinksFuture(q: String = ""): Future[Elem] = {
    val fut = backlinks(q, hrefDisplayPrefix)
    wrapSearchResults(fut, q)
  }

//  private def wrapSearchResults(fut: Future[NodeSeq], q: String, mess:String= "Searched for"): Future[Elem] =
//    fut.map { v =>
//      <section class="label-search-results">
//        <p class="label-search-header">{mess} "{ q }" :</p>
//        <div>
//        { css.localCSS }
//        { v }
//        </div>
//      </section>
//    }

  def esearchFuture(q: String = ""): Future[Elem] = {
    val fut = extendedSearch(q)
    wrapSearchResults(fut, q, mess= "Extended search for")
  }

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] =
    putTriples(uri, link, contentType,
      slug, content, request)

  def makeHistoryUserActions(limit: String, lang: String, request: HTTPrequest): NodeSeq =
    makeTableHistoryUserActions(lang, request)(limit)

}
