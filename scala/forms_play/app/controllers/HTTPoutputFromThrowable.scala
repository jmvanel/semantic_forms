package controllers

import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.mvc. BaseController

import deductions.runtime.services.RecoverUtilities
import org.w3.banana.RDF

trait HTTPoutputFromThrowable[Rdf <: RDF, DATASET] extends RecoverUtilities[Rdf, DATASET]
  with BaseController {

  def errorResultFromThrowable(
    t:               Throwable,
    specificMessage: String    = "ERROR",
    request: Request[_] ): Result = {
    InternalServerError(
      s"""Error '$specificMessage', retry later !!!!!!!!
        ${request.uri}
          ${t.getLocalizedMessage}
          ${printMemory}""")
  }

  def errorActionFromThrowable(
    t:               Throwable,
    specificMessage: String    = "ERROR") = Action {
          implicit request: Request[_] =>
        errorResultFromThrowable(t, specificMessage, request)
    }
}
