package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.mvc.Request

trait RequestUtils extends HTTPrequestHelpers {

  def log(mess: String, request: Request[_]): String =
		  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) +
      s""" $mess: IP ${request.remoteAddress}, userId "${
         copyRequest(request).userId()
        }", $request, id ${request.id}, host ${request.host}"""

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }
}