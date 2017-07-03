package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.mvc.Request

import deductions.runtime.html.Form2HTMLObject
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.user.RegisterPage

/** Dummy controller for helping in creating new web pages or services */
object DummyController extends Controller
    with ImplementationSettings.RDFCache
    with LanguageManagement
    with RegisterPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  override implicit val config = new DefaultConfiguration {}
  override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

  def page() =
    Action { implicit request =>
      val requestCopy = getRequestCopy()
      val userid = requestCopy.userId()
      val title = "My view"
      val lang = chooseLanguage(request)
      val userInfo = displayUser(userid, "", title, lang)
      // outputMainPage()
      Ok("OK from DummyController")
    }

  // TODO move this reutilisable functions to new trait PlayHelpers 
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

}