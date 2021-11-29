package controllers

import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.AnyContent
import play.api.mvc.Results._
import play.api.mvc.AcceptExtractors
import play.api.http.HeaderNames._

import org.w3.banana.RDF

import deductions.runtime.services.Lookup
import deductions.runtime.services.RecoverUtilities
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.jena.ImplementationSettings

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

//class LookupServiceApp  extends  {
//  override implicit val config = new PlayDefaultConfiguration
//} with RDFStoreLocalJenaProvider
//with LookupService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]

//trait LookupService[Rdf <: RDF, DATASET] extends Lookup[Rdf, DATASET]
class LookupServiceApp @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration) extends {
    override implicit val config = new PlayDefaultConfiguration(configuration)
  }
with AbstractController(components)
with RDFStoreLocalJenaProvider
with Lookup[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with LanguageManagement
with HTTPoutputFromThrowable[ImplementationSettings.Rdf, ImplementationSettings.DATASET  ]
with AcceptExtractors {

  private val XML = "application/xml"

  def lookupService(search: String, clas: String = "") = {
    recoverFromOutOfMemoryErrorGeneric[Action[AnyContent]](
      {
      Action { implicit request: Request[AnyContent] =>
        val httpRequest = copyRequest(request)
        logger.info(s"""Lookup: ${httpRequest.logRequest()}
            accepts ${httpRequest.getHTTPheaderValue("Accept")} """)
        val lang = httpRequest.getLanguage
        val mime = httpRequest.firstMimeTypeAccepted(XML)
        logger.debug(s"	First mime $mime")
        Ok(lookup(search, lang, clas, mime)).as(s"$mime; charset=utf-8")
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
    },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in GET /lookup"))
  }
}
