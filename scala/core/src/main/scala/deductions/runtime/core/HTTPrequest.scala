package deductions.runtime.core

import java.net.URLEncoder
import scala.collection.Seq
import java.net.URLDecoder
import scala.xml.NodeSeq

/**
 * Like Request from Play! (in package play.api.mvc), but avoid Play! dependency
 *  [[play.api.mvc.Request]]
 */
case class HTTPrequest(
    /**
     * The HTTP host (domain, optionally port)
     */
    host: String = "localhost",

    /** The client IP address.
     *
     * the last untrusted proxy
     * from the Forwarded-Headers or the X-Forwarded-*-Headers.
     */
    remoteAddress: String = "",

    rawQueryString: String = "",

    queryString: Map[String, Seq[String]] = Map(),

    content: Option[String] = None,

    headers: Map[String, Seq[String]] = Map(),
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
    domain: String = "",
    session: Map[String,String] = Map(),
    username : Option[String] = None
    ) {

  /** get RDF subject, that is "focus" (HTTP parameter "displayuri") */
  def getRDFsubject(): String =
    getHTTPparameterValue("displayuri").getOrElse(
      getHTTPparameterValue("q").getOrElse(""))

  /** is it a locally hosted URI? (that is created by a user by /create ) */
  def isFocusURIlocal(): Boolean = {
    getRDFsubject().startsWith(absoluteURL())
  }

  def getHTTPparameterValue(param: String): Option[String] = queryString.get(param) .map(seq => seq.headOption ) . flatten
  def setDefaultHTTPparameterValue(param: String, value: String): HTTPrequest = {
    getHTTPparameterValue(param) match {
      case Some(v) => this
      case None => this.copy(
        queryString = queryString + (param -> Seq(value)))
    }
  }

  def getHTTPheaderValue(header: String): Option[String] = headers.get(header) .map(seq => seq.headOption ) . flatten

  /** Resolve given relative URI starting with Slash with this request's path */
  def absoluteURL(relativeURIwithSlash: String = "",
      secure: Boolean = this.secure): String =
    "http" + (if (secure) "s" else "") +
      "://" +
      this.host + relativeURIwithSlash // + this.appendFragment

  def originalURL(): String = absoluteURL(path +
      (if(rawQueryString != "") "?" + rawQueryString else ""))

  def adjustSecure(url:String): String =
    if (secure)
      url.replaceFirst("^http://", "https://")
    else
      url.replaceFirst("^https://", "http://")

  def userId(): String = {
    // Play 2.6 & 2.5
    URLDecoder.decode( username.getOrElse("anonymous"), "UTF-8")
  }

  def flashCookie(id: String): String = {
    val x = for (
      cookie <- cookies.get("PLAY_FLASH");
      value = cookie.value
    ) yield {
      // println(s">>>>>>>> flashCookie value $value")
      substringAfter(value, s"$id=") }
    URLDecoder.decode(x . getOrElse(""), "UTF-8")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }

  /** URL Encoded local Sparql Endpoint */
  def localSparqlEndpoint = URLEncoder.encode(absoluteURL("/sparql"), "UTF-8")

  def getLanguage(): String = {
    val resLang = if (acceptLanguages.length > 0) acceptLanguages(0) else "en"
//    logger.debug("chooseLanguage", this) + s"\t$resLang" )
    resLang
  }

  def isLanguageFitting(lang: String): Boolean = {
    acceptLanguages.contains(lang)
  }

  /** like queryString, but returns the first value only for a key */
  def queryString2: Map[String, String] =
    for ( (key, seq) <- queryString ) yield {
      (key, seq.headOption.getOrElse(""))
    }

  override def toString(): String = {
    s"""    host:  = $host,
    remoteAddress: $remoteAddress,
    rawQueryString: $rawQueryString,
    queryString: $queryString,
    content: $content,
    headers: $headers,
    cookies: $cookies,
    acceptLanguages: $acceptLanguages,
    path: $path,
    formMap: $formMap,
    uri: $uri,
    secure: $secure,
    domain: $domain
      """
  }

  private var messages: NodeSeq = NodeSeq.Empty
  def appMessages: NodeSeq = messages
  def addAppMessage(m: NodeSeq): Unit = { messages = messages ++ m }
}

/** Borrowed from Play */
case class Cookie(
  name: String, value: String, maxAge: Option[Int] = None, path: String = "/",
  domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

