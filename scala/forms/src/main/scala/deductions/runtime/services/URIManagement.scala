package deductions.runtime.services

import java.net.InetAddress
import java.net.URLDecoder
import java.net.URLEncoder

import deductions.runtime.utils.URIHelpers

/**
 * Management of URI policy: how are URI created by the application
 *
 * * @author j.m. Vanel
 */
trait URIManagement extends Configuration
    with URIHelpers {

  def makeId: String = {
    makeId(instanceURIPrefix)
  }

  /**
   * make URI From String, if not already an absolute URI,
   *  by prepending instance URI Prefix and URL Encoding
   */
  def makeURIFromString(objectStringFromUser: String): String = {
    if (isAbsoluteURI(objectStringFromUser))
      objectStringFromUser
    else {
      instanceURIPrefix +
        // urlencode takes care of other forbidden character in "candidate" URI
        URLEncoder.encode(objectStringFromUser.replaceAll(" ", "_"), "UTF-8")
    }
  }

  /** make a human readable String From given URI */
  def makeStringFromURI(uri: String): String = {
    lastSegment(URLDecoder.decode(uri, "UTF-8")).replaceAll("_", " ")
  }

  /** make a unique Id with given prefix, currentTimeMillis() and nanoTime() */
  private def makeId(instanceURIPrefix: String): String = {
    instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
  }

  /**
   * NOTE: must not be a val because of test, otherwise Play test says
   *  "There is no started application"
   */
  def instanceURIPrefix: String = {
    val hostNameUsed =
      if (useLocalHostPrefixForURICreation) {
        "http://" + InetAddress.getLocalHost().getHostName()
        // TODO : get the actual port
      } else defaultInstanceURIHostPrefix
    hostNameUsed + ":" + serverPort + "/" + relativeURIforCreatedResourcesByForm
  }

}