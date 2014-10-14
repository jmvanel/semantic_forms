package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Request // [play.api.mvc.AnyContent]
import deductions.runtime.html.TableView

object Application extends Controller with TableView {
  def index = {
    Action {
      (
          Ok( views.html.index(global.Global.form) )
      )
    }
  }

  def displayURI(uri:String, blanknode:String="", Edit:String="") = {
    Action { implicit request =>
      println( "displayURI: " + request )
      println( "displayURI: " + Edit )
      Ok( views.html.index(global.Global.htmlForm(uri, blanknode, editable=Edit!="",
                    lang=chooseLanguage(request) )) )
    }
  }
  
  def wordsearch(q:String="") = {
    Action { (
          Ok( views.html.index(global.Global.wordsearch(q)) )
    ) }
  }
  
  def download( url:String ) = {
    Action { Ok( global.Global.download(url) ).as("text/turtle") }
  }

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
      Ok( views.html.index(global.Global.htmlForm(
          url,
          editable=true,
          lang=chooseLanguage(request)
      )) )
      }
  }

  def save() = {
    Action { implicit request =>
      Ok( views.html.index(global.Global.save(request) )) // .as("text/html")
      // Ok( views.html.index(global.Global.htmlForm(uri, blanknode)) )
    }
  }
  
  def create( uri:String ) = {
    Action { implicit request =>
      println( "create: " + request )
      println( "create: " + uri )
      Ok( views.html.index(global.Global.create(uri, chooseLanguage(request)) ))
    }
  }
}
