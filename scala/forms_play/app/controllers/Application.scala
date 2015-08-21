package controllers

import java.io.OutputStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.xml.NodeSeq

import org.w3.banana.io.JsonLdExpanded
import org.w3.banana.io.JsonLdFlattened
import org.w3.banana.io.RDFWriter
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import com.hp.hpl.jena.query.Dataset

import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.LDP
import deductions.runtime.services.Lookup
import play.api.libs.json.Json
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.Controller


object Application extends Controller
    with JenaModule
    with RDFStoreLocalJena1Provider
    with LanguageManagement
    with LDP[Jena, Dataset]
    with Lookup[Jena, Dataset]
    {

  import scala.util.Try
  // Members declared in org.w3.banana.JsonLDWriterModule
  implicit val jsonldExpandedWriter = new RDFWriter[Rdf,scala.util.Try,JsonLdExpanded] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String]= ???
  }
  implicit val jsonldFlattenedWriter = new RDFWriter[Rdf,scala.util.Try,JsonLdFlattened] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String]= ???
  }


  val glob = _root_.global1.Global

  def index() = {
    Action { implicit request =>
      Ok(views.html.index(glob.form)(lang = chooseLanguageObject(request)))
    }
  }

  def displayURI(uri: String, blanknode: String = "", Edit: String = "") = {
    Action { implicit request =>
      println("displayURI: " + request)
      println("displayURI: " + Edit)
      Ok(views.html.index(glob.htmlForm(uri, blanknode, editable = Edit != "",
        lang = chooseLanguage(request)))).
        withHeaders("Access-Control-Allow-Origin" -> "*") // for dbpedia lookup
    }
  }

  //  def displayURI2(uri: String) = {
  //    Action.async { implicit request =>
  //      println("displayURI2: " + request)
  //      //      Ok.chunked( glob.displayURI2(uri) )
  //      glob.displayURI2(uri) map { x =>
  //        Ok(x.mkString).as("text/html")
  //      }
  //    }
  //  }

  def wordsearch(q: String = "") = Action.async {
    val fut = glob.wordsearchFuture(q)
    fut.map(r => Ok(views.html.index(r)))
  }

  def download(url: String) = {
    Action { Ok(glob.downloadAsString(url)).as("text/turtle; charset=utf-8") }
  }

  /** cf https://www.playframework.com/documentation/2.3.x/ScalaStream */
  def download_chunked(url: String) = {
    Action { Ok.chunked(glob.download(url)).as("text/turtle; charset=utf-8") }
  }

  def edit(url: String) = {
    Action { request =>
      Ok(views.html.index(glob.htmlForm(
        url,
        editable = true,
        lang = chooseLanguage(request)))).
        withHeaders("Access-Control-Allow-Origin" -> "*") // TODO dbpedia only
    }
  }

  def save() = {
    Action { implicit request =>
      Ok(views.html.index(glob.save(request)))
    }
  }

  def create() = {
    Action { implicit request =>
      println("create: " + request)
      val uri = getFirstNonEmptyInMap( request.queryString, "uri" )
      val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formspec")
      println("create: " + uri)
      println("formSpecURI: " + formSpecURI)
      Ok(views.html.index(glob.createElem2(uri, chooseLanguage(request),
          formSpecURI) ) )
    }
  }

  def getFirstNonEmptyInMap(map: Map[String, Seq[String]],
      uri:String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("")
  }

  def sparql(query: String) = {
    Action { implicit request =>
      println("sparql: " + request)
      println("sparql: " + query)
      Ok(views.html.index(glob.sparql(query, chooseLanguage(request))))
    }
  }

  def select(query: String) = {
    Action { implicit request =>
      println("sparql: " + request)
      println("sparql: " + query)
      Ok(views.html.index(glob.select(query, chooseLanguage(request))))
    }
  }

  def backlinks(q: String = "") = Action.async {
    val fut = glob.backlinksFuture(q)
    val extendedSearchLink = <p>
                               <a href={ "/esearch?q=" + q }>
                                 Extended Search for &lt;{ q }
                                 &gt;
                               </a>
                             </p>
    fut.map { res =>
      Ok(views.html.index(NodeSeq
        fromSeq Seq(extendedSearchLink, res)))
    }

  }

  def extSearch(q: String = "") = Action.async {
    val fut = glob.esearchFuture(q)
    fut.map(r => Ok(views.html.index(r)))
  }

  def ldp(uri: String) = {
    Action { implicit request =>
      println("LDP GET: " + request)
      val contentType = request.contentType
      val AcceptsTurtle = Accepting("text/turtle")
      val turtle = AcceptsTurtle.mimeType
      val accepts = Accepting(contentType.getOrElse(turtle))
      val r = getTriples(uri, accepts.mimeType)
      println("LDP: GET: " + r)
      render {
        case AcceptsTurtle() =>
          Ok(r).as( turtle + "; charset=utf-8")
        case Accepts.Json() => Ok(Json.toJson(r))
      }
    }
  }

  /** TODO: this is blocking code !!! */
  def ldpPOST(uri: String) = {
    Action { implicit request =>
      println("LDP: " + request)
      val slug = request.headers.get("Slug")
      val link = request.headers.get("Link")
      val contentType = request.contentType
      val content = {
        val asText = request.body.asText
        if (asText != None) asText
        else {
          val raw = request.body.asRaw.get
          println(s"""LDP: raw: "$raw" size ${raw.size}""")
          raw.asBytes(raw.size.toInt).map {
            arr => new String(arr, "UTF-8")
          }
        }
      }
      println(s"LDP: content: $content")
      val serviceCalled =
        putTriples(uri, link, contentType, slug, content).getOrElse("default")
      Ok(serviceCalled).as("text/plain; charset=utf-8")
    }
  }

  def lookupService(search: String) = {
    Action { implicit request =>
      println("Lookup: " + request)
      Ok(lookup(search)).as("text/json-ld; charset=utf-8")
    }
  }

}
