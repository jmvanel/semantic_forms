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
    acceptLanguages: Seq[String] = Seq()
    ) {

  def absoluteURL(rawURI: String = "", secure: Boolean = false): String =
    "http" + (if (secure) "s" else "") + "://" +
      this.host + rawURI // + this.appendFragment

  def userId(): String = {
    val usernameFromSession = for (
      cookie <- cookies.get("PLAY_SESSION");
      value = cookie.value
    ) yield { substringAfter(value, "username=") }
    usernameFromSession.getOrElse("anonymous")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }
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

