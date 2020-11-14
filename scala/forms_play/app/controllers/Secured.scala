package controllers


import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.Configuration
import deductions.runtime.services.Authentication

import play.api.mvc._
/**
 * Trait for user/password secured controllers
 *  cf https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Security.scala
 */
trait Secured
    extends Authentication[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    // ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Results {

  val config: Configuration
  import config._

  val loginActivated = needLogin

  def getUsername(request: RequestHeader): Option[String] =
    request.session.get(Security.username)

  private def onUnauthorized(request: RequestHeader) = {
    if (request.path.startsWith("/form"))
      Unauthorized("""{"currentRequest": "ERROR UNAUTHORIZED"}""").
        addingToSession("to-redirect" -> request.uri)(request)
    else
      Results.Redirect(routes.AuthService.login).
        addingToSession("to-redirect" -> request.uri)(request)
  }

  /** Ensures the controller is only accessible to registered users */
  private def withAuth(fun: => String => Request[AnyContent] => Result): EssentialAction = {
    Security.Authenticated(getUsername, onUnauthorized) { user =>
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
