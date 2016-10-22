package controllers

import play.api.mvc.Request
import deductions.runtime.utils.HTTPrequest

trait HTTPrequestHelpers {
  def copyRequest(request: Request[_] ): HTTPrequest =
    HTTPrequest(request.host, request.remoteAddress)
}