package deductions.runtime.services

import java.net.URLDecoder
import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import com.hp.hpl.jena.query.Dataset
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

/**
 * a Web Application Facade,
 *  that still exposes to client all dependences on semantic_forms implementations
 *   and Banana
 *
 * API Functions already implemented by inheritance :
 *  def lookup(search: String): String
 *  def login(loginName: String, password: String): Option[String]
 *  def ldpGET(uri: String, accept: String): String = getTriples(uri, accept)
 *
 */
trait ApplicationFacadeImpl[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with TableViewModule[Rdf, DATASET]
    with StringSearchSPARQL[Rdf, DATASET]
    with ReverseLinksSearchSPARQL[Rdf, DATASET]
    with ExtendedSearchSPARQL[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with FormSaver[Rdf, DATASET]
    with CreationFormAlgo[Rdf, DATASET]
    with LDP[Rdf, DATASET]
    with Lookup[Rdf, DATASET]
    with Authentication[Rdf, DATASET] //with ApplicationFacadeInterface
    with RegisterPage[Rdf, DATASET]
    with FormHeader[Rdf]
    with TimeSeries[Rdf, DATASET]
    with ToolsPage {
 
  addSaveListener(this) // for TimeSeries
  
	// TODO use inverse Play's URI API
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="
  val hrefEditPrefix = "/edit?url="
  
  val logger = Logger.getRootLogger()

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
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

  var form: Elem = <p>initial value</p>
  lazy val tableView = this
  lazy val search = this

  lazy val dl = this
  lazy val fs = this
  lazy val cf = this

  import rdfStore.transactorSyntax._



  /** TRANSACTIONAL */
  def htmlForm(uri0: String, blankNode: String = "",
               editable: Boolean = false,
               lang: String = "en"): NodeSeq = {
    Logger.getRootLogger().info(s"""Global.htmlForm URI $uri0 blankNode "$blankNode" lang=$lang """)
    val uri = uri0.trim()
    if (uri != null && uri != "")
      try {
        val res = dataset.rw({
        	val resRetrieve = retrieveURINoTransaction(makeUri(uri), dataset)
        	val status = resRetrieve match {
        	  case Failure(e) => e.getLocalizedMessage
        	  case _ => ""
        	}
          implicit val graph = allNamedGraph;
          Seq(
            titleEditDisplayDownloadLinks(uri, lang),
            <div>{status}</div>,
            tableView.htmlFormElemRaw(uri, graph, hrefDisplayPrefix, blankNode, editable = editable,
              lang = lang)).flatMap { identity }
        })
        res.get
      } catch {
        case e: Exception => // e.g. org.apache.jena.riot.RiotException
          <p style="color:red">
            {
              e.getLocalizedMessage() + " " + printTrace(e)
            }<br/>
            Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
          </p>
      }
    else
      <div class="row">Enter an URI</div>
  }

  private def htmlFormOLD(uri0: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en")
    : Elem = {
    Logger.getRootLogger().info(s"""Global.htmlForm URI $uri0 blankNode "$blankNode" lang=$lang """)
    val uri = uri0.trim()

    <div class="container">
      { //titleEditDisplayDownloadLinks(uri, lang)
      }
      {
        if (uri != null && uri != "")
          try {
//        	  tableView.htmlFormElem(uri, hrefDisplayPrefix, blankNode, editable = editable,
//        			  lang = lang)
        	  val res = dataset.rw({
        		  tableView.htmlFormElemRaw(uri, allNamedGraph, hrefDisplayPrefix, blankNode, editable = editable,
        				  lang = lang)
        	  })
        	  res.get
          } catch {
            case e: Exception => // e.g. org.apache.jena.riot.RiotException
              <p style="color:red">
                {
                  e.getLocalizedMessage() + " " + printTrace(e)
                }<br/>
                Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
              </p>
          }
        else
          <div class="row">Enter an URI</div>
      }
    </div>
  }


  /** NON transactional */
  def labelForURI(uri: String, language: String)
  (implicit graph: Rdf#Graph)
    : String = {
      instanceLabel(URI(uri), graph, language)
  }
  /** NOTE this creates a transaction; do not use it too often;
   * will not be used */
  def labelForURITransaction(uri: String, language: String)
  : String = {
    rdfStore.r(dataset, {
      instanceLabel(URI(uri), allNamedGraph, language)
    }).getOrElse(uri)
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

  def wordsearchFuture(q: String = "", lang: String = ""): Future[Elem] = {
    val fut = searchString(q, hrefDisplayPrefix, lang)
    wrapSearchResults(fut, q)
  }

  def downloadAsString(url: String): String = {
    println("download url " + url)
    val res = dl.focusOnURI(url)
    println(s"""download result "$res" """)
    res
  }

  /** TODO should be non-blocking !!!!!!!!!!!!!
   *  currently accumulates a string first !!!
   *  not sure if Banana and Jena allow a non-blocking access to SPARQL query results */
  def download(url: String): Enumerator[Array[Byte]] = {
	  val res = downloadAsString(url)
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

  def saveForm(request: Map[String, Seq[String]], lang: String = "") // (implicit allNamedGraphs: Rdf#Graph)
  : NodeSeq = {
    println("ApplicationFacade.save: map " + request)
    try {
      fs.saveTriples(request)
      ("noUserURI") // TODO get user ID when login activated
    } catch {
      case t: Throwable =>
        println("Exception in saveTriples: " + t)
        // show Exception to user:
        throw t
    }
    val uriOption = (request).getOrElse("uri", Seq()).headOption
    println("Global.save: uriOption " + uriOption)
    uriOption match {
      case Some(url1) =>
      val uri = URLDecoder.decode(url1, "utf-8")
      val res = dataset.rw({
        replaceInstanceLabel( URI(uri), allNamedGraph, // TODO reuse allNamedGraph
            lang )       
    	})
        htmlForm( uri,
        editable = false,
        lang = lang)
      case _ => <p>Save: not normal: { uriOption }</p>
    }
  }

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("Global.sparql query  " + query)
    <p>
		{ sparqlQueryForm(query, "/sparql",
				"CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10" ) }
      <pre>
        {
          try {
        	  if( query != "" )
        		  dl.sparqlConstructQueryTR(query)
          } catch {
            case t: Throwable => t.printStackTrace() // TODO: handle error
          }
          /* TODO Future !!!!!!!!!!!!!!!!!!! */
        }
      </pre>
    </p>
  }

  /*
  def sparqlQueryForm(query: String, action: String, sampleQuery: String): NodeSeq =
    <form role="form" action={action}>
    SPARQL query:
    <textarea name="query" cols="80">
      {if( query != "" )
        query
      else
        "# " + sampleQuery }
    </textarea>
    <input type="submit" value="Submit"/>
    </form>
  */
  
  def selectSPARQL(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("sparql query  " + query)
    <p>
		{ sparqlQueryForm(query, "/select",
				"SELECT * WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10" ) }
      <br></br>
      <script type="text/css">
        table {{ border-collapse:collapse; width:90%; }}
        th, td {{ border:1px solid black; width:20%; }}
        td {{ text-align:center; }}
       caption {{ font-weight:bold }}
      </script>
      <table>
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
  private def wrapSearchResults(fut: Future[Elem], q: String): Future[Elem] =
    fut.map { v =>
      <section class="label-search-results">
        <p class="label-search-header">Searched for "{ q }" :</p>
        <div>{ v }</div>
      </section>
    }

  def esearchFuture(q: String = ""): Future[Elem] = {
    val fut = extendedSearch(q)
    wrapSearchResults(fut, q)
  }

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String]): Try[String] =
    putTriples(uri, link, contentType,
      slug, content)

}
