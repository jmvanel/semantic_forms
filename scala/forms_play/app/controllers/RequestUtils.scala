package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsText
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.AnyContentAsRaw
import play.api.mvc.RawBuffer
import play.api.mvc.AnyContentAsXml
import play.api.mvc.AnyContentAsJson

trait RequestUtils extends HTTPrequestHelpers {

  /** Format log message */
  def log(mess: String, request: Request[_]): String =
		  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) +
      s""" $mess: IP ${request.remoteAddress}, userId "${
         copyRequest(request).userId()
        }", $request, id ${request.id}, host ${request.host}"""

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }

  /** @return in case of Form Url Encoded, the first value in Map,
   *  or in case of AnyContentAsRaw, the raw content,
   *  or in case of XML, the XML content,
   *  or in case of JSON, the JSON content,
   *   */
  protected def getContent(request: Request[AnyContent]): Option[String] = {
    request.body match {
      case AnyContentAsText(t) => Some(t)
      case AnyContentAsFormUrlEncoded(m) =>
        println(s"getContent 1 request.body AnyContentAsFormUrlEncoded size ${m.size}")
        m.headOption  match {
          case Some(v) =>
            println(s"getContent 1.1 param '${v._1}'")
            v . _2 . headOption
          case None => None
        }
      case AnyContentAsRaw(raw: RawBuffer) =>
        println(s"getContent 2 request.body.asRaw ${raw}")
        raw.asBytes(raw.size.toInt).map {
          arr => new String(arr.toArray, "UTF-8")
        }

      case AnyContentAsXml(xml) => Some(xml.toString())
      case AnyContentAsJson(c) => Some(c.toString())

      case _ => None
    }
  }
}