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

class LookupServiceApp  extends  {
  override implicit val config = new PlayDefaultConfiguration
} with RDFStoreLocalJenaProvider
with LookupService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]

trait LookupService[Rdf <: RDF, DATASET] extends Lookup[Rdf, DATASET]
with LanguageManagement
with HTTPoutputFromThrowable[Rdf, DATASET]
with AcceptExtractors {

  def lookupService(search: String, clas: String = "") = {
    recoverFromOutOfMemoryErrorGeneric[Action[AnyContent]](
      {
      Action { implicit request: Request[AnyContent] =>
        logger.info(s"""Lookup: $request
            accepts ${request.acceptedTypes} """)
        val lang = chooseLanguage(request)
        val mime = request.acceptedTypes.headOption.map {
          typ => typ.toString() }.getOrElse(Accepts.Xml.mimeType)
        logger.info(s"mime $mime")
        Ok(lookup(search, lang, clas, mime)).as(s"$mime; charset=utf-8")
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
    },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in GET /lookup"))
  }
}