package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Request
import deductions.runtime.html.TableView
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with TableView {
  val glob = _root_.global.Global
  
  def index = {
    Action { Ok( views.html.index(glob.form) ) }
  }

  def displayURI(uri:String, blanknode:String="", Edit:String="") = {
    Action { implicit request =>
      println( "displayURI: " + request )
      println( "displayURI: " + Edit )
      Ok( views.html.index(glob.htmlForm(uri, blanknode, editable=Edit!="",
                    lang=chooseLanguage(request) )) )
    }
  }

  def wordsearch(q:String="") = wordsearchNEW(q)

  def wordsearchOLD(q:String="") = {
    Action { Ok( views.html.index(glob.wordsearch(q)) ) }
  }
  
  /** TODO !!!!!!!!!!!!!!!!!!!! */
  def wordsearchNEW(q:String="") = Action.async {
    val f = glob.wordsearchFuture(q)
    val res = f.map( r => Ok(views.html.index(r)) )
    res
  }
  
  def download0( url:String ) = {
//    Action { Ok.chunked( glob.download(url) ).as("text/turtle") }
    Action { Ok( glob.downloadAsString(url) ).as("text/turtle") }
  }

  /** cf https://www.playframework.com/documentation/2.3.x/ScalaStream */
  def download( url:String ) = {
    Action { Ok.chunked( glob.download(url) ).as("text/turtle") }
//    Action { Ok.stream( glob.download(url) ).as("text/turtle") }
  }
    
  /* Ok.stream(enumerator >>> Enumerator.eof).
   * 
   */
  def chooseLanguage(request: Request[_]): String = {
    //     val l1 = request.headers.get("Accept-Language")
    val languages = request.acceptLanguages
    val res = if (languages.length > 0) languages(0).language else "en"
    	println("chooseLanguage: " + request + "\n\t" + res)
    	res
  }
  
  def edit( url:String ) = {
    Action {
        request =>
      Ok( views.html.index(glob.htmlForm(
          url,
          editable=true,
          lang=chooseLanguage(request)
      )) )
      }
  }

  def save() = {
    Action { implicit request =>
      Ok( views.html.index(glob.save(request) )) // .as("text/html")
      // Ok( views.html.index(glob.htmlForm(uri, blanknode)) )
    }
  }
  
  def create( uri:String ) = {
    Action { implicit request =>
      println( "create: " + request )
      println( "create: " + uri )
      Ok( views.html.index(glob.create(uri, chooseLanguage(request)) ))
    }
  }
  
  def sparql( query: String) = {
        Action { implicit request =>
      println( "sparql: " + request )
      println( "sparql: " + query )
      Ok( views.html.index(glob.sparql( query, chooseLanguage(request)) ))
    }
  }
}
