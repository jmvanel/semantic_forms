package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Request
import deductions.runtime.html.TableView
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import scala.xml.NodeSeq
import play.api.i18n.Lang

object Application extends Controller with TableView
with JenaHelpers
with RDFStoreLocalJena1Provider {
  val glob = _root_.global1.Global

  def index() = {
    Action { implicit request =>
      Ok(views.html.index(glob.form)(lang = chooseLanguageObject(request))) }
  }

  def displayURI(uri: String, blanknode: String = "", Edit: String = "") = {
    Action { implicit request =>
      println("displayURI: " + request)
      println("displayURI: " + Edit)
      Ok(views.html.index(glob.htmlForm(uri, blanknode, editable = Edit != "",
        lang = chooseLanguage(request)))).
        withHeaders( "Access-Control-Allow-Origin" -> "*" ) // for dbpedia lookup
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
    Action { Ok(glob.downloadAsString(url)).as("text/turtle, charset=utf-8") }
  }

  /** cf https://www.playframework.com/documentation/2.3.x/ScalaStream */
  def download_chunked(url: String) = {
    Action { Ok.chunked(glob.download(url)).as("text/turtle, charset=utf-8") }
  }

  def chooseLanguage(request: Request[_]): String = {
    chooseLanguageObject(request).language
  }
  def chooseLanguageObject(request: Request[_]): Lang = {
    val languages = request.acceptLanguages
    val res = if (languages.length > 0) languages(0) else Lang("en")
    println("chooseLanguage: " + request + "\n\t" + res)
    res
  }

  def edit(url: String) = {
    Action { request =>
        Ok(views.html.index(glob.htmlForm(
          url,
          editable = true,
          lang = chooseLanguage(request)
        ))).
        withHeaders( "Access-Control-Allow-Origin" -> "*" ) // TODO dbpedia only
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
      val uri = getFirstNonEmptyInMap(request.queryString) . get
      println("create: " + uri)
      Ok(views.html.index(glob.createElem2(uri, chooseLanguage(request))))
    }
  }

  /** TODO move to FormSaver */
  def getFirstNonEmptyInMap(map: Map[String, Seq[String]]): Option[String] = {
    val uriArgs = map.getOrElse("uri", Seq())
    uriArgs.find { uri => uri != "" }
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
  
  def backlinks(q:String = "") = Action.async {
    val fut = glob.backlinksFuture(q)
    val extendedSearchLink = <p>
    <a href={"/esearch?q="+q}>
    Extended Search for &lt;{q}&gt;</a>
    </p>
    fut.map{ res => Ok(views.html.index( NodeSeq
        fromSeq Seq(extendedSearchLink, res))) }

  }

  def extSearch(q:String = "") = Action.async {
    val fut = glob.esearchFuture(q)
    fut.map(r => Ok(views.html.index(r)))
  }

}
