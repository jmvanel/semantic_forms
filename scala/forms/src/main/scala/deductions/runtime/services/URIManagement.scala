package deductions.runtime.services

import java.net.InetAddress
import java.net.URLDecoder
import java.net.URLEncoder

import deductions.runtime.utils.URIHelpers
import java.net.NetworkInterface
import scala.collection.JavaConversions._
import java.net.Inet4Address
import deductions.runtime.utils.HTTPrequest
import java.net.URI

/**
 * Management of URI policy: how are URI created by the application
 *
 * * @author j.m. Vanel
 */
trait URIManagement extends URIHelpers {

  val config: Configuration
//  import config._
  import config.defaultInstanceURIHostPrefix
  import config.relativeURIforCreatedResourcesByForm
  import config.serverPort
  import config.useLocalHostPrefixForURICreation

  def makeId(request: HTTPrequest): String = {
    makeId(instanceURIPrefix(request))
  }

  /**
   * make URI From String, if not already an absolute URI,
   *  by prepending instance URI Prefix and URL Encoding
   */
  def makeURIFromString(stringFromUser: String): String = {
    if (isAbsoluteURI(stringFromUser))
      stringFromUser
    else {
      instanceURIPrefix +
        makeURIPartFromString(stringFromUser)
    }
  }

  /** urlencode takes care of other forbidden character in "candidate" URI */
  def makeURIPartFromString(stringFromUser: String): String =
    if (stringFromUser.contains("@"))
      stringFromUser
    else
      URLEncoder.encode(stringFromUser.replaceAll(" ", "_"), "UTF-8")

  /**
   * make Absolute URI For Saving in user named graph:
   *  add URI scheme mailto or user, if not already absolute
   */
  def makeAbsoluteURIForSaving(userid: String): String = {
    val uri = new URI(userid)
    if (uri.isAbsolute())
      userid
    else {
      (if (userid.contains("@"))
         "mailto:"
       else
         "user:"
      ) + userid
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

  def instanceURIPrefix(request: HTTPrequest): String = {
    val hostname =
      if (request.host != "" // if one wants to avoid localhost (but why?)
      //    && request.host.contains(".")
      )
        request.host
      else request.remoteAddress

    val serverPortPart = if (hostname.contains(":")) "" else ":" + serverPort
    val urip = "http://" + hostname + serverPortPart + "/" + relativeURIforCreatedResourcesByForm
    println1(s"instance URI Prefix <$urip> - $request")
    urip
  }

  /**
   * if host Name From API contains "." , it is not a global Internet DNS, then use the IP for created URI's .
   * @DEPRECATED
   *
   * NOTE: must not be a val because of test, otherwise Play test says
   *  "There is no started application"
   */
  def servicesURIPrefix: String = {
    servicesURIPrefix2._1
  }

  /** @return services URI Prefix, is DNS (not IP) */
  def servicesURIPrefix2: (String, Boolean) = {

    val (hostNameUsed, isDNS: Boolean) =
      if (useLocalHostPrefixForURICreation) {
        val hostNameFromAPI = InetAddress.getLocalHost().getCanonicalHostName // getHostName()
        println( s"hostNameFromAPI $hostNameFromAPI")
        if (hostNameFromAPI.contains("."))
          "http://" + hostNameFromAPI
        else {
          // the server is not registered on DNS or in /etc/hosts : get the actual IPV4 port
          val nis = NetworkInterface.getNetworkInterfaces().toList
          val adresses = for (
            networkInterface <- nis;
            _ = println1(getNetworkInterfaceInfo(networkInterface));
            adress <- networkInterface.getInetAddresses;
            hostAddress = adress.getHostAddress;
            _ = println1(s"ni $networkInterface hostAddress $hostAddress")
          ) yield adress

          val internetAdresses = adresses.filter { adress =>
            val hostAddress = adress.getHostAddress;
            (!adress.isLoopbackAddress &&
              !hostAddress.contains(":") &&
              !hostAddress.startsWith("10.") &&
              !hostAddress.startsWith("192.168.")
            )
          }
          val intranetAdresses = adresses.filter { adress =>
            val hostAddress = adress.getHostAddress;
            (!adress.isLoopbackAddress &&
              !hostAddress.contains(":") && (
                hostAddress.startsWith("10.") ||
                hostAddress.startsWith("192.168."))
            )
          }
          println1(s"intranetAdresses $intranetAdresses")
          println1(s"internetAdresses $internetAdresses")
          val (adresses2, isDNS) = if (!internetAdresses.isEmpty) {
            if (internetAdresses.size > 1)
              System.err.println(s"CAUTION: several Internet Adresses: $internetAdresses")
            (internetAdresses, true)
          } else {
            if (intranetAdresses.size > 1)
              System.err.println(s"CAUTION: several Intranet Adresses: $intranetAdresses")
            (intranetAdresses, false)
          }
          val result = "http://" + adresses2.toList.headOption.getOrElse("127.0.0.1")
          // "http://" + InetAddress.getLocalHost().getHostAddress()
          (result, isDNS)
        }
      } else (defaultInstanceURIHostPrefix, true)
    println1(s"hostNameUsed $hostNameUsed")
    ( hostNameUsed + ":" + serverPort + "/" , isDNS)
  }

  def instanceURIPrefix: String = servicesURIPrefix + relativeURIforCreatedResourcesByForm

  def getNetworkInterfaceInfo(netif: NetworkInterface): String = {
    if (netif == null) {
      return "Invalid network interface.";
    }

    try {
      return "Interface name: " + netif.getDisplayName() + "\n" +
        "Device: " + netif.getName() + "\n" +
        "Is loopback: " + netif.isLoopback() + "\n" +
        "Is up: " + netif.isUp() + "\n" +
        "Is p2p: " + netif.isPointToPoint() + "\n" +
        "Is virtual: " + netif.isVirtual() + "\n" +
        "Supports multicast: " + netif.supportsMulticast() + "\n" +
        //                "MAC address: " + getMacAddress(netif) + "\n" +
        "IP addresses: " + getIPv4Addresses(netif);
    } catch {
      case t: Throwable =>
        return "Failed to get network interface information.";

    }
  }

  /**
   * Returns a list of the IPv4-addresses on the network interface in string format.
   * from http://www.programcreek.com/java-api-examples/index.php?api=java.net.NetworkInterface
   *
   * @param netif The network interface to get the IPv4-addresses from.
   * @return All the IPv4-addresses on the network interface.
   */
  def getIPv4Addresses(netif: NetworkInterface): String = {
    if (netif == null) {
      return "";
    }
    var ipAddress = "";
    val netAddresses = netif.getInetAddresses();
    for (inetAddress <- netAddresses) {
      if (inetAddress.isInstanceOf[Inet4Address]) {
        ipAddress = ipAddress + inetAddress.getHostAddress() +
          s" isAnyLocalAddress ${inetAddress.isAnyLocalAddress()} isLinkLocalAddress ${inetAddress.isLinkLocalAddress} - "
      } else
        ipAddress = ipAddress + inetAddress.getClass + " - "
    }
    return ipAddress;
  }

  def println1(mess: String) = if (false) println(mess)

}