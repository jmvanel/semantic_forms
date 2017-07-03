package controllers

import play.api.mvc.Request
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.utils.Cookie

/** copy PLay! classes into SF classes, to avoid dependencies in other project senantic_forms */
trait HTTPrequestHelpers {
  
	/** a copy of the request with no Play dependency :) */
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

  def copyRequest(request: Request[_]): HTTPrequest = {
    import request._
    val cookiesMap = cookies.map { cookie => (cookie.name -> copyCookie(cookie)) } . toMap
    val res = HTTPrequest(host, remoteAddress,
      rawQueryString, queryString,
      headers = headers.toMap, cookies = cookiesMap
      )
//    println(s"copyRequest: cookiesMap $cookiesMap , userId ${res.userId()}")
    res
  }

  def copyCookie(cookie: play.api.mvc.Cookie): Cookie = {
    import cookie._
    Cookie(name, value: String, maxAge: Option[Int], path: String,
      domain: Option[String], secure: Boolean, httpOnly)
  }
}

