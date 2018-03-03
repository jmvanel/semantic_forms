package deductions.runtime.utils

import java.net._

import scala.collection.JavaConverters._
import deductions.runtime.core.HTTPrequest

import scalaz._
import Scalaz._

/**
 * Management of URI policy: how are URI created by the application
 *
 * * @author j.m. Vanel
 */
trait URIManagement extends URIHelpers {

  val config: Configuration
//  import config._
  import config.{defaultInstanceURIHostPrefix, relativeURIforCreatedResourcesByForm, serverPort, useLocalHostPrefixForURICreation}

  def makeId(request: HTTPrequest): String = {
    makeId(instanceURIPrefix(request))
  }

  /**
   * make URI From String, if not already an absolute URI,
   *  by prepending instance URI Prefix and URL Encoding
   */
  def makeURIFromString(stringFromUser: String, predicate: String=""): String = {
    if (isAbsoluteURI(stringFromUser))
      stringFromUser
    else if(predicate === "http://xmlns.com/foaf/0.1/phone" )
      "tel:" + stringFromUser .replaceAll(" ", "_")
      // NOTE schema:telephone is an xsd:string
    else if(stringFromUser.contains("@"))
      "mailto:" + stringFromUser .replaceAll(" ", "")
    else {
      if (stringFromUser != "")
        instanceURIPrefix +
          makeURIPartFromString(stringFromUser)
      else ""
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
    try {
      val uri = new URI(userid)
      if (uri.isAbsolute())
        userid
      else {
        (if (userid.contains("@"))
          "mailto:"
        else
          "user:") + userid
      }
    } catch {
      case t: Throwable =>
        System.err.println(s"makeAbsoluteURIForSaving: $userid: ${t.getLocalizedMessage}")
        "error:error"
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

  /** @return services URI Prefix, is DNS (not IP)
   *  TODO : also take in account the HTTPrequest class */
  def servicesURIPrefix2: (String, Boolean) = {

    val (hostNameUsed: String, isDNS: Boolean) =
      if (useLocalHostPrefixForURICreation) {
        val hostNameFromAPI = InetAddress.getLocalHost().getCanonicalHostName // getHostName()
        println( s"servicesURIPrefix2: hostNameFromAPI <$hostNameFromAPI>")

        if (hostNameFromAPI.contains("."))
          ("http://" + hostNameFromAPI, true)

        else {
          // the server is not registered on DNS or in /etc/hosts : get the actual IPV4 port
          val nis = NetworkInterface.getNetworkInterfaces().asScala.toList
          val adresses = for (
            networkInterface <- nis;
            _ = println1("servicesURIPrefix2: "+getNetworkInterfaceInfo(networkInterface));
            adress <- networkInterface.getInetAddresses.asScala ;
            hostAddress = adress.getHostAddress;
            _ = println1(s"servicesURIPrefix2: ni $networkInterface hostAddress $hostAddress")
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
          println1(s"servicesURIPrefix2: intranetAdresses $intranetAdresses")
          println1(s"servicesURIPrefix2: internetAdresses $internetAdresses")
          val (adresses2, isDNS) = if (!internetAdresses.isEmpty) {
            if (internetAdresses.size > 1)
              System.err.println(s"CAUTION: several Internet Adresses: $internetAdresses")
            (internetAdresses, true)
          } else {
            if (intranetAdresses.size > 1)
              System.err.println(s"CAUTION: servicesURIPrefix2: several Intranet Adresses: $intranetAdresses")
            (intranetAdresses, false)
          }
          println1(s"servicesURIPrefix2: adresses2 $adresses2")
          val result = {
            val adress22 = adresses2.toList.headOption.getOrElse("127.0.0.1")
            "http://" + adress22.toString().replaceFirst("^/", "")
          }
          // "http://" + InetAddress.getLocalHost().getHostAddress()
          (result, isDNS)
        }
      } else (defaultInstanceURIHostPrefix, true)
    println1(s"servicesURIPrefix2: hostNameUsed <$hostNameUsed>")
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
    for (inetAddress <- netAddresses.asScala) {
      if (inetAddress.isInstanceOf[Inet4Address]) {
        ipAddress = ipAddress + inetAddress.getHostAddress() +
          s" isAnyLocalAddress ${inetAddress.isAnyLocalAddress()} isLinkLocalAddress ${inetAddress.isLinkLocalAddress} - "
      } else
        ipAddress = ipAddress + inetAddress.getClass + " - "
    }
    return ipAddress;
  }

  def println1(mess: String) = if (logActive) println(mess)

}
