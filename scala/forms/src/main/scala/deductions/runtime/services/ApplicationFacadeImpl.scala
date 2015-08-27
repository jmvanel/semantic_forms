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

import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.CreationFormAlgo
import deductions.runtime.html.TableViewModule
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.I18NMessages

//import deductions.runtime.services.Lookup{ lookup => lookup0 }

import play.api.libs.iteratee.Enumerator

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
trait ApplicationFacadeImpl[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET]
    with TableViewModule[Rdf, DATASET]
    with StringSearchSPARQL[Rdf, DATASET]
    with ReverseLinksSearchSPARQL[Rdf, DATASET]
    with ExtendedSearchSPARQL[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with FormSaver[Rdf, DATASET]
    with CreationFormAlgo[Rdf, DATASET]
    with LDP[Rdf, DATASET]
    with Lookup[Rdf, DATASET]
    with Authentication[Rdf, DATASET]
//with ApplicationFacadeInterface
{

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  import ops._

  Logger.getRootLogger().info(s"in Global")

  var form: Elem = <p>initial value</p>
  lazy val tableView = this
  lazy val search = this

  lazy val dl = this
  lazy val fs = this
  lazy val cf = this
  lazy val allNamedGraphs = allNamedGraph

  // TODO use inverse Play's URI API
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="
  val hrefEditPrefix = "/edit?url="

  /** TODO move some formatting to views or separate function */
  def htmlForm(uri0: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en")(implicit allNamedGraphs: Rdf#Graph): Elem = {
    Logger.getRootLogger().info(s"""Global.htmlForm uri $uri0 blankNode "$blankNode" lang=$lang """)
    val uri = uri0.trim()

    <div class="container">
      <div class="container">
        <div class="row">
          <h3>
            { I18NMessages.get("Properties_for", lang) }
            <b>
              <a href={ hrefEditPrefix + URLEncoder.encode(uri, "utf-8") } title="edit this URI">
                { labelForURI(uri, lang) }
              </a>
              , URI :
              <a href={ hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8") } title="display this URI">{ uri }</a>
              <a href={ s"/backlinks?q=${URLEncoder.encode(uri, "utf-8")}" } title="links towards this URI">o--></a>
            </b>
          </h3>
        </div>
        <div class="row">
          <div class="col-md-6">
            <a href={ uri } title="Download from original URI">Download from original URI</a>
          </div>
          <div class="col-md-6">
            <a href={ hrefDownloadPrefix + URLEncoder.encode(uri, "utf-8") } title="Download Turtle from database (augmented by users' edits)">Triples</a>
          </div>
        </div>
      </div>
      {
        if (uri != null && uri != "")
          try {
            tableView.htmlFormElem(uri, hrefDisplayPrefix, blankNode, editable = editable,
              lang = lang)
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

  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String = {
    rdfStore.r(dataset, {
      instanceLabel(URI(uri), allNamedGraphs, language)
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

  def wordsearchFuture(q: String = ""): Future[Elem] = {
    val fut = searchString(q, hrefDisplayPrefix)
    wrapSearchResults(fut, q)
  }

  def downloadAsString(url: String): String = {
    println("download url " + url)
    val res = dl.focusOnURI(url)
    println("download result " + res)
    res
  }

  def download(url: String): Enumerator[Array[Byte]] = {
    // cf https://www.playframework.com/documentation/2.3.x/ScalaStream
    // and http://greweb.me/2012/11/play-framework-enumerator-outputstream/
    Enumerator.outputStream { os =>
      //        val dl = new BrowsableGraph[Rdf, DATASET]{}
      val graph = search_only(url)
      graph.map { graph =>
        /* non blocking */
        val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
        val ret = writer.write(graph, os, base = url)
        os.close()
      }
    }
  }

  def edit(url: String)(implicit allNamedGraphs: Rdf#Graph): Elem = {
    htmlForm(url, editable = true)
  }

  def saveForm(request: Map[String, Seq[String]], lang: String = "")(implicit allNamedGraphs: Rdf#Graph): Elem = {
    println("ApplicationFacade.save: map " + request)
    try {
      fs.saveTriples(request)
    } catch {
      case t: Throwable =>
        println("Exception in saveTriples: " + t)
        // show Exception to user:
        throw t
    }
    val uriOption = (request).getOrElse("uri", Seq()).headOption
    println("Global.save: uriOption " + uriOption)
    uriOption match {
      case Some(url1) => htmlForm(
        URLDecoder.decode(url1, "utf-8"),
        editable = false,
        lang = lang)
      case _ => <p>Save: not normal: { uriOption }</p>
    }
  }

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("Global.sparql query  " + query)
    <p>
      SPARQL query:<br/>{ query }
      <br/>
      <pre>
        {
          try {
            dl.sparqlConstructQuery(query)
          } catch {
            case t: Throwable => t.printStackTrace() // TODO: handle error
          }
          /* TODO Future !!!!!!!!!!!!!!!!!!! */
        }
      </pre>
    </p>
  }

  def selectSPARQL(query: String, lang: String = "en"): Elem = {
    Logger.getRootLogger().info("sparql query  " + query)
    <p>
      SPARQL query:<pre>{ query }</pre>
      <br></br>
      <script type="text/css">
        table {{
 border-collapse:collapse;
 width:90%;
 }}
th, td {{
 border:1px solid black;
 width:20%;
 }}
td {{
 text-align:center;
 }}
caption {{
 font-weight:bold
 }}
      </script>
      <table>
        {
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
      </table>
    </p>
  }

  def backlinksFuture(q: String = ""): Future[Elem] = {
    val fut = backlinks(q, hrefDisplayPrefix)
    wrapSearchResults(fut, q)
  }

  private def wrapSearchResults(fut: Future[Elem], q: String): Future[Elem] =
    fut.map { v =>
      <p>
        Searched for "{ q }
        " :<br/>
        { v }
      </p>
    }

  def esearchFuture(q: String = ""): Future[Elem] = {
    val fut = extendedSearch(q)
    wrapSearchResults(fut, q)
  }

  def ldpPUT(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String]): Try[String] =
    putTriples(uri, link, contentType,
      slug, content)

}
