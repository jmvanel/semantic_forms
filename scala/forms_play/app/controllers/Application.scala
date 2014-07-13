package controllers

import play.api._
import play.api.mvc._
import deductions.runtime.html.TableView

object Application extends Controller with TableView {
//  val uri = "/home/jmv/jmvanel.free.fr/jmv.rdf"
//  val f =  htmlFormString(uri)
  def index = {
    Action {
      (
//          Ok( views.html.index(f ) )
//          Ok( "views.html.index(f )" )
          Ok( views.html.index(Global.form) )
      )
    }
  }
}
