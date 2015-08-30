package controllers

import play.api._
import play.api.mvc._
//import play.api.data.Form
//import play.api.data.Forms._
import deductions.runtime.services.Authentication
import org.w3.banana.RDF

/** Trait for user/password secured controllers
 *  cf https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Security.scala */
trait Secured[Rdf <: RDF, DATASET] extends Authentication[Rdf, DATASET] {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  /** Ensures the controller is only accessible to registered users */
  private def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /** Ensures authentication and passes the user to the controller */
  def withUser( fun: String => Request[AnyContent] => Result) = withAuth { username =>
    implicit request =>
      findUser(username).map { user =>
        fun(user)(request)
      }.getOrElse(onUnauthorized(request))
  }
}