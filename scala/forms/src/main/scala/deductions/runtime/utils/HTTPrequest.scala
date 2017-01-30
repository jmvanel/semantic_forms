package deductions.runtime.utils

import java.net.URLEncoder

/**
 * Like Request from Play! (in package play.api.mvc), but avoid Play! dependency
 *  [[play.api.mvc.Request]]
 */
case class HTTPrequest(
    /**
     * The HTTP host (domain, optionally port)
     */
    host: String = "",

    /**
     * The client IP address.
     *
     * the last untrusted proxy
     * from the Forwarded-Headers or the X-Forwarded-*-Headers.
     */
    remoteAddress: String = "",

    rawQueryString: String = "",

    /** YET UNUSED */
    headers: Map[String, Seq[String]] = Map(),
    cookies: List[Cookie] = List()) {

  def absoluteURL(rawURI: String = "", secure: Boolean = false): String =
    "http" + (if (secure) "s" else "") + "://" +
      this.host + rawURI // + this.appendFragment

  def localSparqlEndpoint = URLEncoder.encode(absoluteURL("/sparql"), "UTF-8")

}

/** Borrowed from Play */
case class Cookie(
  name: String, value: String, maxAge: Option[Int] = None, path: String = "/",
  domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

