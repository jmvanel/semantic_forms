package controllers

import play.api.mvc.Request
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.Cookie
import play.api.mvc.AnyContentAsFormUrlEncoded

/** copy PLay! classes into SF classes, to avoid dependencies in other project senantic_forms */
trait HTTPrequestHelpers {
  
	/** a copy of the request with no Play dependency :) */
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

  def copyRequest(request: Request[_]): HTTPrequest = {
    import request._
    val cookiesMap = cookies.map { cookie => (cookie.name -> copyCookie(cookie)) } . toMap
    val formMap = getFormMap(request)
    val res = HTTPrequest(host, remoteAddress,
      rawQueryString, queryString,
      headers = headers.toMap,
      cookies = cookiesMap,
      acceptLanguages = request.acceptLanguages . map {
        al => al.language
      },
      path = request.path,
      formMap = formMap
    )
    println(s"copyRequest: headers: " + headers.toMap)
//    println(s"copyRequest: cookiesMap $cookiesMap , userId ${res.userId()}")
    res
  }

  def copyCookie(cookie: play.api.mvc.Cookie): Cookie = {
    import cookie._
    Cookie(name, value: String, maxAge: Option[Int], path: String,
      domain: Option[String], secure: Boolean, httpOnly)
  }

  def getFormMap(request: Request[_]):  Map[String, Seq[String]] = {
    val body = request.body
    body match {
      case form: AnyContentAsFormUrlEncoded => form.data
      case _ => Map()
    }
  }
}

