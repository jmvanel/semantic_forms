package controllers

import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.utils.DefaultConfiguration
import play.api.Play
import deductions.runtime.utils.FormModuleBanana
import deductions.runtime.jena.ImplementationSettings

//import javax.inject.Inject
//import play.api.mvc.ControllerComponents


/** main Application controller */
object Application extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  with Services
  with WebPages
  with SparqlServices
  with FormModuleBanana[ImplementationSettings.Rdf] {
  override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
}
