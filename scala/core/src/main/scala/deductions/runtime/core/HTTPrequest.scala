package deductions.runtime.core

import java.net.URLEncoder
// import scala.collection.Seq
import java.net.URLDecoder
import scala.xml.NodeSeq
import java.net.URI
import java.net.InetAddress

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
    username : Option[String] = None,
    method: String = ""

    ) {

  /** get RDF subject, that is "focus" (HTTP parameter "displayuri") */
  def getRDFsubject: String =
    getHTTPparameterValue("displayuri").getOrElse(
      getHTTPparameterValue("q").getOrElse(
        getHTTPparameterValue("url").
          getOrElse("")))

  def getQueries() =
    getHTTPparameterValues("q")

  private def getHostOfRDFsubject(): String = {
    val subjectURI = getRDFsubject.trim
    if (subjectURI.startsWith("_:"))
      ""
    else
      new URI(subjectURI).getHost
  }

  /** is the RDF subject (=focus URI) a locally hosted URI?
   *  (that is created by a user by /create )
   *  accept URI's differing only on http versus https */
  def isFocusURIlocal(): Boolean = {
    val hostOfRDFsubject = getHostOfRDFsubject()
    logger.debug(s""">>>> isFocusURIlocal: getHostOfRDFsubject: <$hostOfRDFsubject> =? hostNoPort <$hostNoPort> ,
      remoteAddress [$remoteAddress]
      uri <$uri>""")
    val isFocusURIlocal =
    hostOfRDFsubject == hostNoPort ||
    hostOfRDFsubject == s"[$remoteAddress]"
    logger.debug(s""">>>> isFocusURIlocal: $isFocusURIlocal ,
      uri.contains("%2Fjson2rdf ${uri.contains("%2Fjson2rdf%3F")}""")
    // /json2rdf/
    isFocusURIlocal && ! uri.contains("%2Fjson2rdf%3F")
  }

  private def removeHTTPprotocolFromURI(uri:String): String =
    uri.replaceFirst("https?://", "")

  /** get (first) HTTP parameter Value (and trim it) */
  def getHTTPparameterValue(param: String): Option[String] =
    queryString.get(param) .map(seq => seq.headOption map(s => s.trim)) . flatten

  def getHTTPparameterValueOrEmpty(param: String): String =
    getHTTPparameterValue(param).getOrElse("")

  /** get HTTP parameter Values */
  def getHTTPparameterValues(param: String): Seq[String] =
    queryString.get(param).headOption.getOrElse(Seq() )

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
        s"http${if(secure) "s" else ""}://" +
      {
        logger.debug(s"""absoluteURL this.remoteAddress ${this.remoteAddress} this.host $host
            hostNoPort $hostNoPort relativeURIwithSlash $relativeURIwithSlash port $port""" )
        if (this.hostNoPort == "localhost" ||
            this.hostNoPort . startsWith("[") ) {
          logger.debug(s"IPV6 or IPV4 normalize address, getInetAddress ${getInetAddress}")
          if(isIPv6())
            "[" + getInetAddress.getHostAddress + "]:" + this.port
          else
            getInetAddress.getHostAddress + ":" + this.port
        }
        else this.host
      } +
        relativeURIwithSlash // + this.appendFragment

  private def getInetAddress = InetAddress.getByName(remoteAddress)
  private def isIPv6() = {
    getInetAddress.getAddress.size > 4
  }

  def originalURL(): String = absoluteURL(path +
      (if(rawQueryString != "") "?" + rawQueryString else ""))

  def originalURLNoQuery(): String = absoluteURL(path)

  def adjustSecure(url:String): String =
    if (secure)
      url.replaceFirst("^http://", "https://")
    else
      url.replaceFirst("^https://", "http://")

  def userId: String = {
    // Play 2.6 & 2.5
    URLDecoder.decode( username.getOrElse("anonymous"), "UTF-8")
  }

  def flashCookie(id: String): String = {
    val x = for (
      cookie <- cookies.get("PLAY_FLASH");
      value = cookie.value
    ) yield {
      // logger.debug(s">>>>>>>> flashCookie value $value")
      substringAfter(value, s"$id=") }
    URLDecoder.decode(x . getOrElse(""), "UTF-8")
  }

  def substringAfter(s: String, k: String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i + k.length) } }

  /** URL Encoded local Sparql Endpoint */
  def localSparqlEndpoint = URLEncoder.encode(absoluteURL("/sparql"), "UTF-8")

  def getLanguage: String = {
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

  def hostNoPort: String = {
    // take in account IPV6 , e.g. [::1]:9000
    val colonIndex = host.lastIndexOf(":")
    if( colonIndex == -1 )
      host
    else
      host.subSequence(0, colonIndex).toString()
  }

  def port = {
    val colonIndex = host.lastIndexOf(":")
    if( colonIndex == -1 )
      80
    else
      host.substring(colonIndex+1)
  }

  def firstMimeTypeAccepted(default: String="") : String = {
    val accepts = getHTTPheaderValue("Accept")
    accepts.getOrElse(default).replaceFirst(",.*", "")
  }

  override def toString(): String = {
    s"""    host:  = $host,
    remoteAddress: $remoteAddress,
    rawQueryString: $rawQueryString,
    queryString: $queryString,
    username $username,
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

  def logRequest() = s"$uri, host <$host>, RDF subject <$getRDFsubject>, IP $remoteAddress, userId '$userId', lang '$getLanguage'"
}

/** Borrowed from Play */
case class Cookie(
  name: String, value: String, maxAge: Option[Int] = None, path: String = "/",
  domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

