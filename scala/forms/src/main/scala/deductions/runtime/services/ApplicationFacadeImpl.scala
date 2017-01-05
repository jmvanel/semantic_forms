package deductions.runtime.services

import java.net.URLDecoder
import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.Text

import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.CreationFormAlgo
import deductions.runtime.html.TableViewModule
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.I18NMessages
import play.api.libs.iteratee.Enumerator
import org.w3.banana.io.JsonLdExpanded
import java.io.OutputStream
import org.w3.banana.io.JsonLdFlattened
import deductions.runtime.user.RegisterPage
import scala.util.Try
import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import java.io.ByteArrayInputStream
import scala.util.Failure
import deductions.runtime.views.FormHeader
import deductions.runtime.views.ToolsPage
import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.semlogs.LogAPI
import deductions.runtime.html.CSS
import deductions.runtime.data_cleaning.BlankNodeCleanerIncremental
import scala.util.control.NonFatal
import scala.util.control.NonFatal
import org.w3.banana.io.JsonLdCompacted
import deductions.runtime.sparql_cache.algos.StatisticsGraph
import deductions.runtime.utils.HTTPrequest

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
    with TableViewModule[Rdf, DATASET]
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
    with FormHeader[Rdf]
    with TimeSeries[Rdf, DATASET]
    with NamedGraphsSPARQL[Rdf, DATASET]
    with TriplesInGraphSPARQL[Rdf, DATASET]
    with BlankNodeCleanerIncremental[Rdf, DATASET]
    with DashboardHistoryUserActions[Rdf, DATASET]
    with StatisticsGraph[Rdf]
    with FormJSON[Rdf, DATASET]
    with ToolsPage
//    with Configuration
    with CSS
    {
 
  val config: Configuration
  import config._

  //  val v = new TimeSeries[Rdf, DATASET]{}
  //  if( activateUserInputHistory )
  addSaveListener(this) // for TimeSeries

  
  val logger = Logger.getRootLogger()

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]

  import ops._

//  val ops1 = ops
//  addSaveListener(new LogAPI[Rdf] {
//    implicit val ops = ops1
//    def notifyDataEvent(addedTriples: Seq[Rdf#Triple],
//    		removedTriples: Seq[Rdf#Triple])(implicit userURI: String): Unit = {
//      addedTriples.headOption match {
//        case Some(tr) => instanceLabel( tr.subject, graph, lang) 
//      }
//    }
//  } )
  
  // Members declared in org.w3.banana.JsonLDWriterModule
  implicit val jsonldExpandedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdExpanded] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }
  implicit val jsonldFlattenedWriter = new RDFWriter[Rdf, scala.util.Try, JsonLdFlattened] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String] = ???
  }

  Logger.getRootLogger().info(s"in Global")

  lazy val search = this

  lazy val dl = this
  lazy val fs = this
  lazy val cf = this

  import rdfStore.transactorSyntax._



  /** Naked form from TableView, plus:
   *
   * - title and links on top of the form,
   * - page URI (graph) statistics,
   * - behavior (manageBlankNodesReload, Exception management)
   *
   * TODO extract as WrappedHTMLForm
   * TRANSACTIONAL */
  def htmlForm(uri0: String, blankNode: String = "",
               editable: Boolean = false,
               lang: String = "en", formuri: String="",
               graphURI: String = "",
               database: String = "TDB" ): NodeSeq = {
    Logger.getRootLogger().info(
        s"""ApplicationFacadeImpl.htmlForm URI $uri0 blankNode "$blankNode"
              editable=$editable lang=$lang graphURI <$graphURI>""")
    val uri = uri0.trim()
    if (uri != null && uri != "")
      try {
        val datasetOrDefault = getDatasetOrDefault(database)
        val res = rdfStore.rw( datasetOrDefault, {
          val status = if (blankNode != "true") {
            val resRetrieve = retrieveURINoTransaction(
              // if( blankNode=="true") makeUri("_:" + uri ) else makeUri(uri),
              makeUri(uri), datasetOrDefault)

            // TODO should be done in FormSaver 
            println(s"Search in $uri duplicate graph rooted at blank node: size " +
                ops.getTriples(resRetrieve.get).size)
            manageBlankNodesReload(resRetrieve.getOrElse(emptyGraph),
                URI(uri), datasetOrDefault)

            val status = resRetrieve match {
              case Failure(e) => e.getLocalizedMessage
              case Success(g) => formatHTMLStatistics(URI(uri), g, lang)
            }
            status
          } else ""

          implicit val graph = allNamedGraph
          val formBoth = htmlFormElemRaw(uri, graph, hrefDisplayPrefix, blankNode, editable = editable,
              lang = lang,
              formuri = formuri,
              graphURI = graphURI,
              database = database)
          val formItself = formBoth . _1
          val formSyntax = formBoth . _2

            Text("\n") ++
            titleEditDisplayDownloadLinks(uri, lang, editable) ++
//            titleEditDisplayDownloadLinksThumbnail(formSyntax, lang, editable) ++ // TODO <<<<<<<<<<<<
            <div>{status}</div> ++
            formItself
        })
        res.get
      } catch {
        case e: Exception => // e.g. org.apache.jena.riot.RiotException
          <p style="color:red">
        <pre>
            {
              e.getLocalizedMessage() + " " + printTrace(e).replaceAll( "\\)", ")\n")
            }<br/>
            Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
            </pre>
          </p>
      }
    else
      <div class="row">Enter an URI</div>
  }

  /** NON transactional */
  def labelForURI(uri: String, language: String)
  (implicit graph: Rdf#Graph)
    : String = {
      instanceLabel(URI(uri), graph, language)
  }
  /** NOTE this creates a transaction; do not use it too often */
  def labelForURITransaction(uri: String, language: String)
  : String = {
//    println( s"labelForURITransaction $uri, $language"  )
    val res = rdfStore.r(dataset, {
      instanceLabel(URI(uri), allNamedGraph, language)
    }).getOrElse(uri)
//    println( s"result $res"  )
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

  def printTrace(e: Exception): String = {
    var s = ""
    for (i <- e.getStackTrace()) { s = s + " " + i }
    s
  }

  def wordsearchFuture(q: String = "", lang: String = "", clas: String = ""): Future[Elem] = {
    val fut = searchString(q, hrefDisplayPrefix, lang, clas)
    wrapSearchResults(fut, q)
  }

  def rdfDashboardFuture(q: String = "", lang: String = ""): Future[NodeSeq] = {
    val fut = showNamedGraphs(lang)
    wrapSearchResults(fut, q)
  }
    
  def downloadAsString(url: String, mime: String="text/turtle"): String = {
    println( s"download url $url mime $mime")
    val res = dl.focusOnURI(url, mime)
    println(s"""download result "$res" """)
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
      println(s"after search_only($url)")
      val r = graph.map { graph =>
        /* non blocking */
        val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
        println("before writer.write()")
        val ret = writer.write(graph, os, base = url)
        println("after writer.write()")
        os.close()
      }
      println("after graph.map()")
    }
  }

  def edit(url: String) // (implicit allNamedGraphs: Rdf#Graph)
  : NodeSeq = {
    htmlForm(url, editable = true)
  }

  /** save Form data in TDB
   *  @return main subject URI like [[FormSaver.saveTriples]] */
  def saveForm(request: Map[String, Seq[String]], lang: String = "",
      userid: String, graphURI: String = "", host: String= "")
  : Option[String] = {
//    println(s"ApplicationFacadeImpl.save: map :$request, userid <$userid>")
    val mainSubjectURI = try {
      implicit val userURI: String = userid
      fs.saveTriples(request)
    } catch {
      case t: Throwable =>
        println("Exception in saveTriples: " + t)
        throw t
    }
    val uriOption = (request).getOrElse("uri", Seq()).headOption
    println(s"ApplicationFacadeImpl.save: uriOption $uriOption, graphURI $graphURI")
    uriOption match {
      case Some(url1) =>
      val uri = URLDecoder.decode(url1, "utf-8")
      val res = dataset.rw({
        replaceInstanceLabel( URI(uri), allNamedGraph, // TODO reuse allNamedGraph
            lang )
    	})
    	logger.info( s"Save: normal! $uriOption" )
      case _ => logger.info( s"Save:  NOT normal! $uriOption" )
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

  /** XHTML wrapper around SPARQL result */
  def sparqlConstructQuery(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("Global.sparql query  " + query)
    <p>
		{ sparqlQueryForm(query, "/sparql",
				Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10") ) }
      <pre>
        {
          try {
        	  if( query != "" )
        		  dl.sparqlConstructQueryTR(query)
          } catch {
            case NonFatal(e) => e.printStackTrace() // TODO: handle error?
          }
          /* TODO Future !!!!!!!!!!!!!!!!!!! */
        }
      </pre>
    </p>
  }

  /** SPARQL result
   *  @param format = "turtle" or "rdfxml" or "jsonld" */
  def sparqlConstructResult(query: String, lang: String = "en", format: String="turtle"): String = {
    Logger.getRootLogger().info("Global.sparql query  " + query)
    try {
      if (query != "")
        sparqlConstructQueryTR(query, format)
      else "Empty query !!!"
    } catch {
      case NonFatal(e) => "sparqlConstructResult: " + e.getMessage() // TODO: handle error?
    }
  }
  
  /** Display result of a SPARQL select */
  def selectSPARQL(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("sparql query  " + query)
    <p>
		{ sparqlQueryForm(query, "/select-ui",
				Seq("SELECT * WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10" )) }
      <br></br>
      <!--script type="text/css">
        table {{ border-collapse:collapse; width:90%; }}
        th, td {{ border:1px solid black; width:20%; }}
        td {{ text-align:center; }}
       caption {{ font-weight:bold }}
      </script-->

      <style type="text/css">
      {cssRules}
      </style>

      <table class="sf-sparql-table">

        { if( query != "" ) {
          val rowsTry = dl.sparqlSelectQuery(query)
          rowsTry match {
            case Success(rows) =>
              val printedRows = for (row <- rows) yield {
                <tr>
                  { for (cell <- row) yield <td> { cell } </td> }
                </tr>
              }
              printedRows
            case Failure(e) => e.toString()
          }
          }
        }
      </table>
    </p>
  }

  def backlinksFuture(q: String = ""): Future[Elem] = {
    val fut = backlinks(q, hrefDisplayPrefix)
    wrapSearchResults(fut, q)
  }

  /** TODO should be in package hmtl */
  private def wrapSearchResults(fut: Future[NodeSeq], q: String, mess:String= "Searched for"): Future[Elem] =
    fut.map { v =>
      <section class="label-search-results">
        <p class="label-search-header">{mess} "{ q }" :</p>
        <div>
        { css.localCSS }
        { v }
        </div>
      </section>
    }

  def esearchFuture(q: String = ""): Future[Elem] = {
    val fut = extendedSearch(q)
    wrapSearchResults(fut, q, mess= "Extended search for")
  }

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] =
    putTriples(uri, link, contentType,
      slug, content, request)

  def makeHistoryUserActions(userURI: String, lang: String, request: HTTPrequest): NodeSeq =
    makeTableHistoryUserActions(lang, request)(userURI)

}
