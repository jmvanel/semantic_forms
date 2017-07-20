package controllers

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.services.{CentralSemanticController, TypicalSFDependencies}
import deductions.runtime.user.RegisterPage
import deductions.runtime.utils.DefaultConfiguration
import play.api.mvc.{Action, Controller, Request}
import deductions.runtime.mobion.GeoController
import deductions.runtime.mobion.PerVehicleView

object SemanticController extends Controller
    with ImplementationSettings.RDFCache
    with CentralSemanticController[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LanguageManagement
    with RegisterPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  val actionMap: Map[String, deductions.runtime.core.SemanticController] = {
    val actions = Seq(
        new {
    override implicit val config = new DefaultConfiguration {
      override val useTextQuery = false
    }
} with TypicalSFDependencies with GeoController[Rdf, DATASET] {},
        new {
    override implicit val config = new DefaultConfiguration {
      override val useTextQuery = false
    }
} with TypicalSFDependencies with PerVehicleView[Rdf, DATASET] {}
    )

    actions.map{ action => (action.featureURI, action) } . toMap
  }

  override implicit val config = new DefaultConfiguration {}
  override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

  def page() =
    Action { implicit request: Request[_] =>
      val requestCopy = getRequestCopy()
      val userid = requestCopy.userId()
      val title = "SemanticController view"
      val lang = chooseLanguage(request)
      val userInfo = displayUser(userid, "", title, lang)
      // outputMainPage()
      Ok( result(requestCopy) ) .as("text/html; charset=utf-8")
    }
}