package deductions.runtime.services

import java.net.InetAddress
import java.net.URLDecoder
import java.net.URLEncoder

import deductions.runtime.utils.URIHelpers
import java.net.NetworkInterface
import scala.collection.JavaConversions._

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
  def makeId(instanceURIPrefix: String): String = {
    instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
  }

  /**
   * if host Name From API contains "." , it is not a global Internet DNS, then use the IP for created URI's .
   *
   * NOTE: must not be a val because of test, otherwise Play test says
   *  "There is no started application"
   */
  def instanceURIPrefix: String = {
    val hostNameUsed =
      if (useLocalHostPrefixForURICreation) {
        val hostNameFromAPI = InetAddress.getLocalHost().getHostName()
        if (hostNameFromAPI.contains("."))
          "http://" + InetAddress.getLocalHost().getHostName()
        else {
          // get the actual port
          val nis = NetworkInterface.getNetworkInterfaces()
          val adresses = for (
            networkInterface <- nis;
            adress <- networkInterface.getInetAddresses;
            hostAddress = adress.getHostAddress;
            zz = println(s"ni $networkInterface hostAddress $hostAddress");
            // if (!hostAddress.startsWith("127."))
            if (!adress.isLoopbackAddress &&
              !hostAddress.contains(":"))
          ) yield adress
          val result = "http://" + adresses.toList.headOption.getOrElse("127.0.0.1")
          // "http://" + InetAddress.getLocalHost().getHostAddress()
          println(s"hostNameUsed $result")
          result
        }
      } else defaultInstanceURIHostPrefix
    hostNameUsed + ":" + serverPort + "/" + relativeURIforCreatedResourcesByForm
  }

}