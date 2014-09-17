package controllers

import play.api._
import play.api.mvc._
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
      Ok( views.html.index(global.Global.htmlForm(uri, blanknode, editable=Edit!="" )) )
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
  
  def edit( url:String ) = {
    Action {
      Ok( views.html.index(global.Global.htmlForm(
          url,
          editable=true
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
      Ok( views.html.index(global.Global.create(uri) ))
    }
  }
}
