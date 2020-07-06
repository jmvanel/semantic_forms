package deductions.runtime.utils

import java.net.URI
import java.net.InetAddress

trait URIHelpers {

  def isAbsoluteURI(uri: String) = {
    try {
      val u = new java.net.URI(uri)
      u.isAbsolute()
    } catch {
      case t: Throwable => false
    }
  }

  def isCorrectURI(uri: String): Boolean = {
    try {
      val u = new java.net.URI(uri)
      true
    } catch {
      case t: Throwable => false
    }
  }

  /** like the Banana function */
  def lastSegment(uri: String): String = {
    try {
      val path = new URI(uri).getPath
      val i = path.lastIndexOf('/')
      if (i < 0)
        path
      else
        path.substring(i + 1, path.length)
    } catch {
      case t: Throwable => uri
    }
  }

  /** url must start with http: ftp: file: https: */
  def isDownloadableURL(url: String) = {
      url.startsWith("http:") ||
      url.startsWith("ftp:") ||
      url.startsWith("file:") ||
      url.startsWith("https:")
  }


  private val documentMIMEs = Set(
      "text/html",
      "application/pdf",
      "application/vnd.oasis.opendocument.text"
  ) // TODO word :(

  /** is a Document in a broad sense, so will typed foaf:Document */
  def isDocumentMIME(mime: String) = {
    val res = documentMIMEs.contains(mime)
    // logger.debug(s"isDocumentMIME(mime=$mime => $res")
    res
  }

  /** normalize URI <scheme>://<authority><path>?<query>
   *  yet UNUSED ! */
  def normalizeURI(uri: URI) : URI = {
//     URI(String scheme,
//               String userInfo, String host, int port,
//               String path, String query, String fragment)
    new URI( uri.getScheme, "",
    InetAddress.getByName(uri.getHost).getHostAddress(),
    uri.getPort,
    uri.getPath,
    uri.getQuery,"")
  }
}
