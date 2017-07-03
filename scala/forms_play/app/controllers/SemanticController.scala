package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.mvc.Request

import deductions.runtime.html.Form2HTMLObject
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.CentralSemanticController
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.user.RegisterPage

object SemanticController extends Controller
    with ImplementationSettings.RDFCache
    with CentralSemanticController[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LanguageManagement
    with RegisterPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  val actionMap: Map[String, deductions.runtime.services.SemanticController] = Map()
  override implicit val config = new DefaultConfiguration {}
  override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

  def page() =
    Action { implicit request =>
      val requestCopy = getRequestCopy()
      val userid = requestCopy.userId()
      val title = "SemanticController view"
      val lang = chooseLanguage(request)
      val userInfo = displayUser(userid, "", title, lang)
      // outputMainPage(
      Ok("OK")
    }

  // TODO move these reutilisable functions to new trait PlayHelpers 
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

}