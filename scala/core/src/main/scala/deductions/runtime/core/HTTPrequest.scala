package deductions.runtime.core

import java.net.URLEncoder
import scala.collection.Seq
import java.net.URLDecoder

/**
 * Like Request from Play! (in package play.api.mvc), but avoid Play! dependency
 *  [[play.api.mvc.Request]]
 */
case class HTTPrequest(
    /**
     * The HTTP host (domain, optionally port)
     */
    host: String = "localhost",

    /* The client IP address.
     *
     * the last untrusted proxy
     * from the Forwarded-Headers or the X-Forwarded-*-Headers.
     */
    remoteAddress: String = "",

    rawQueryString: String = "",

    queryString: Map[String, Seq[String]] = Map(),

    content: Option[String] = None,

    /* YET UNUSED */
    headers: Map[String, Seq[String]] = Map(),
//    cookies: List[Cookie] = List())
    cookies: Map[String, Cookie] = Map(),
    acceptLanguages: Seq[String] = Seq(),
    path: String="",
    /** Form Map from HTTP body in case of HTTP POST with Url Encoded Form, that is
     *  application/x-www-form-urlencoded;
     *  see https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST */
    formMap: Map[String, Seq[String]] = Map(),
    uri: String = "",
    to_string: String = "",
    secure: Boolean= false,
    domain: String = ""
    ) {

  /** get RDF subject (HTTP parameter "displayuri") */
  def getRDFsubject(): String =
    getHTTPparameterValue("displayuri").getOrElse(
      getHTTPparameterValue("q").getOrElse(""))

  def getHTTPparameterValue(param: String): Option[String] = queryString.get(param) .map(seq => seq.headOption ) . flatten
  def setDefaultHTTPparameterValue(param: String, value: String): HTTPrequest = {
    getHTTPparameterValue(param) match {
      case Some(v) => this
      case None => this.copy(
        queryString = queryString + (param -> Seq(value)))
    }
  }

  def getHTTPheaderValue(header: String): Option[String] = headers.get(header) .map(seq => seq.headOption ) . flatten

  /** Resolve given relative URI with Slash with this request's path */
  def absoluteURL(relativeURIwithSlash: String = "",
      secure: Boolean = this.secure): String =
    "http" + (if (secure) "s" else "") +
      "://" +
      this.host + relativeURIwithSlash // + this.appendFragment

  def adjustSecure(url:String): String =
    if (secure)
      url.replaceFirst("^http://", "https://")
    else
      url.replaceFirst("^https://", "http://")

      def userId(): String = {
    val usernameFromSession = for (
      cookie <- cookies.get("PLAY_SESSION");
      value = cookie.value
    ) yield { substringAfter(value, "username=") }
    URLDecoder.decode(
      usernameFromSession.getOrElse("anonymous"), "UTF-8")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }

  /** URL Encoded local Sparql Endpoint */
  def localSparqlEndpoint = URLEncoder.encode(absoluteURL("/sparql"), "UTF-8")

  def getLanguage(): String = {
    val languages = acceptLanguages
    val resLang = if (languages.length > 0) languages(0) else "en"
//    logger.info(
//        log("chooseLanguage", this) + s"\t$resLang" )
    resLang
  }
}

/** Borrowed from Play */
case class Cookie(
  name: String, value: String, maxAge: Option[Int] = None, path: String = "/",
  domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

