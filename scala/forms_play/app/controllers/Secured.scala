package controllers

import play.api._
import play.api.mvc._
import deductions.runtime.services.Authentication
import org.w3.banana.RDF
import deductions.runtime.jena.ApplicationFacadeJena

/**
 * Trait for user/password secured controllers
 *  cf https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Security.scala
 */
trait Secured
    extends ApplicationFacadeJena {

  // TODO loginActivated should be abstract
  val loginActivated = false



  def username(request: RequestHeader) = request.session.get(Security.username)

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  /** Ensures the controller is only accessible to registered users */
  private def withAuth(fun: => String => Request[AnyContent] => Result): EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => fun(user)(request))
    }
  }
  
  /** Ensures authentication and passes the user to the controller */
  def withUser(fun: String => Request[AnyContent] => Result) =
    if(loginActivated)
      withAuth { username =>
        implicit request =>
          findUser(username).map { user =>
            fun(user)(request)
          }.getOrElse(onUnauthorized(request))
      }
    else
      Action(implicit request => fun("")(request))

}