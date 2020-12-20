package controllers

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import deductions.runtime.services.CORS
import deductions.runtime.services.RDF2JSON
import play.api.mvc.Request
import scala.util.Success
import scala.util.Failure

class RDF2JSONservice @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration)
  extends { override implicit val config = new PlayDefaultConfiguration(configuration) } with AbstractController(components)
  with HTTPrequestHelpers
  with RDF2JSON
  with CORS {

  def rdf2json() = Action { implicit request: Request[_] =>
    val httpRequest = copyRequest(request)
    val processed = result(httpRequest)
    processed match {
      case Success(r) =>
        Ok(r)
      case Failure(f) =>
        f.printStackTrace
        val mess = f.getLocalizedMessage
        InternalServerError(mess) //  fillInStackTrace())
    }
  }
}