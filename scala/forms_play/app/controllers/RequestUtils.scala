package controllers

import java.text.DateFormat
import java.util.Date
import play.api.mvc.Request

trait RequestUtils {
  def log(mess: String, request: Request[_]) =
    DateFormat.getInstance.format(new Date()) +
      s" $mess: IP ${request.remoteAddress} $request, id ${request.id}, host ${request.host}"

  def userId(request: Request[_]) = {
    val usernameFromSession = for (
      cookie <- request.cookies.get("PLAY_SESSION");
      value = cookie.value
    ) yield { substringAfter(value, "username=") }
    usernameFromSession.getOrElse("anonymous")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }
}