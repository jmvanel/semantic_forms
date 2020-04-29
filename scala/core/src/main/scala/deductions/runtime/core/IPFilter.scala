package deductions.runtime.core

import scala.xml.NodeSeq
import scala.io.Source
import scala.util.Try
import java.net.InetAddress

trait HTTPFilter {
  /** HTTP Filter
   *  @return a message for HTTP output or None */
  def filter(request: HTTPrequest): Option[String]
}

/** Simple Filter for blacklisting clients
 *  Not satisfied with the API ! :(
 *  Should be able to pipe filters. */
trait IPFilter extends HTTPFilter {

  lazy val blacklistedIPs = {
    val ipsFromFile = Try {
      val bufferedSource = Source.fromFile("blacklist.txt")
      val lines = (for (line <- bufferedSource.getLines()) yield line).toList
      bufferedSource.close
      lines
    }
    ipsFromFile getOrElse List()
  }

  val responseToBlackListed = Some(
  "Black listed, please respect robots.txt, see https://en.wikipedia.org/wiki/Robots_exclusion_standard")

  /** @return a message for HTTP output or None */
  def filter(request: HTTPrequest): Option[String] = {
    val blacklistCriterium = blacklistedIPs contains request.remoteAddress
    if( blacklistCriterium ) {
        responseToBlackListed
    } else {
      val addr = InetAddress.getByName(request.remoteAddress);
      val host = addr.getHostName()
      if( host.endsWith( "compute.amazonaws.com.cn" ) )
        responseToBlackListed
      else None
    }
  }
}

trait SemanticControllerWrapper {

  def filterRequest(request: HTTPrequest, controller: SemanticController, filter: HTTPFilter): SemanticController = {
    new SemanticController {
      override val featureURI = controller.featureURI
      override def result(request: HTTPrequest) =
        filterRequestResult(
          request,
          () => controller.result(request),
          filter)
    }
  }

  import scala.xml.Text
  def filterRequestResult(
    request:    HTTPrequest,
    content: () => NodeSeq, filter: HTTPFilter): NodeSeq =
    filter.filter(request) match {
      case None          => content()
      case Some(message) => Text(message)
    }
}
