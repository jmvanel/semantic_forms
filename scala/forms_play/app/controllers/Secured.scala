package controllers

import play.api._
import play.api.mvc._
import deductions.runtime.services.Authentication
import org.w3.banana.RDF
import deductions.runtime.jena.ApplicationFacadeJena
import deductions.runtime.services.Configuration
/**
 * Trait for user/password secured controllers
 *  cf https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Security.scala
 */
trait Secured
    extends ApplicationFacadeJena
   with Configuration {

  val loginActivated = needLogin


  def username(request: RequestHeader) = request.session.get(Security.username)

  private def onUnauthorized(request: RequestHeader) =
    Results.Redirect(routes.Auth.login).
        addingToSession( "to-redirect" -> request.uri )(request)

  /** Ensures the controller is only accessible to registered users */
  private def withAuth(fun: => String => Request[AnyContent] => Result): EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => fun(user)(request))
    }
  }
  
  /** Ensures authentication and passes the user to the controller
   *  TODO distinguish
   *  needLoginForEditing, needLoginForDisplaying
   *  */
  def withUser(fun: /*user*/ String => Request[AnyContent] => Result): EssentialAction =
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
