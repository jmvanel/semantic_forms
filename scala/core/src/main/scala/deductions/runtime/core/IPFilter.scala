package deductions.runtime.core

import scala.xml.NodeSeq
import scala.xml.Text
import scala.io.Source
import scala.util.Try

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

  /** @return a message for HTTP output or None */
  def filter(request: HTTPrequest): Option[String] = {
    if( blacklistedIPs contains request.remoteAddress ) {
      Some(
        "Black listed, please respect robots.txt, see https://en.wikipedia.org/wiki/Robots_exclusion_standard")
    } else
      None
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

  def filterRequestResult(
    request:    HTTPrequest,
    content: () => NodeSeq, filter: HTTPFilter): NodeSeq =
    filter.filter(request) match {
      case None          => content()
      case Some(message) => Text(message)
    }
}
