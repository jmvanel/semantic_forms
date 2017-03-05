package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.mvc.Request

trait RequestUtils {

  def log(mess: String, request: Request[_]) =
		  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) +
      s""" $mess: IP ${request.remoteAddress}, userId "${userId(request)}", $request, id ${request.id}, host ${request.host}"""

  def userId(request: Request[_]): String = {
    val usernameFromSession = for (
      cookie <- request.cookies.get("PLAY_SESSION");
      value = cookie.value
    ) yield { substringAfter(value, "username=") }
    usernameFromSession.getOrElse("anonymous")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }
}