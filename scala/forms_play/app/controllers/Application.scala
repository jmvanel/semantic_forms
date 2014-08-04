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

  def displayURI(uri:String, blanknode:String="") = {
    Action {
      (
          Ok( views.html.index(global.Global.htmlForm(uri, blanknode)) )
      )
    }
  }
  
  def wordsearch(q:String="") = {
    Action { (
          Ok( views.html.index(global.Global.wordsearch(q)) )
    ) }
  }
  
  def download( url:String ) = {
    Action { (
        Ok( global.Global.download(url) )
    ) }
    }
}
