package controllers.semforms.services

import scala.xml.Elem

import org.apache.log4j.Logger

import deductions.runtime.html.CreationForm
import deductions.runtime.html.TableView
import deductions.runtime.services.FormSaver
import play.api.mvc.Action
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Controller
import play.api.mvc.Request

object ApplicationCRUD extends Controller with TableView with ApplicationCommons {

	lazy val fs = new FormSaver()
  lazy val cf = new CreationForm { actionURI = "/save" }

  def display(uri: String, blanknode: String = "" ) = {
    Action { implicit request =>
      println("display URI: " + request)
      Ok( htmlFormElem( uri, blanknode, editable = false,
        lang = chooseLanguage(request))).
        as("text/html").
        withHeaders( "Access-Control-Allow-Origin" -> "*" )
    }
  }

  def edit(uri: String) = {
    Action { request =>
        Ok( htmlFormElem(
          uri, editable = true,
          lang = chooseLanguage(request)
        )).
        as("text/html").
        withHeaders( "Access-Control-Allow-Origin" -> "*" )
    }
  }
  
  def save() = {
    Action { implicit request =>
      Ok( saveRequest(request) )
    }
  }

  def saveRequest(request: Request[_]): Elem = {
      val body = request.body
      body match {
        case form: AnyContentAsFormUrlEncoded =>
          val map = form.data
          println("save: " + body.getClass + ", map " + map)
          try {
            fs.saveTriples(map)
          } catch {
            case t: Throwable => println("Exception in saveTriples: " + t)
            throw t
          }
          val uriOption = map.getOrElse("uri", Seq()).headOption
          println("save: uriOption " + uriOption)
          uriOption match {
            case Some(url1) => <p>Saved normally: { uriOption }</p>
            case _ => <p>Save: not normal: { uriOption }</p>
          }
        case _ => <p>Save: not normal: { request.toString() }</p>
      }
    }
      
  def create() = {
    Action { implicit request =>
      println("create: " + request)
      val uri0 = getFirstNonEmptyURIInMap(request.queryString) . get
      println("create: " + uri0)
      Logger.getRootLogger().info("Global.htmlForm uri " + uri0)
      val uri = uri0.trim()
      Ok( cf.create(uri, chooseLanguage(request)).get ).
        as("text/html")
    }
  }

  /** TODO move to FormSaver */
  private def getFirstNonEmptyURIInMap(map: Map[String, Seq[String]]): Option[String] = {
    val uriArgs = map.getOrElse("uri", Seq())
    uriArgs.find { uri => uri != "" }
  }

}
