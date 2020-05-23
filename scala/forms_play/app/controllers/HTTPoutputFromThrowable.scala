package controllers

import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.Results._
import deductions.runtime.services.RecoverUtilities

trait HTTPoutputFromThrowable extends RecoverUtilities {

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