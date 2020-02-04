package deductions.runtime.core

import scala.xml.NodeSeq
import scala.xml.Text

trait HTTPFilter {
  /** HTTP Filter
   *  @return a message for HTTP output or None */
  def filter(request: HTTPrequest): Option[String]
}

/** Simple Filter for blacklisting clients
 *  Not satisfied with the API ! :(
 *  Should be able to pipe filters. */
trait IPFilter extends HTTPFilter {
  /** @return a message for HTTP output or None */
  def filter(request: HTTPrequest): Option[String] = {
    if( request.remoteAddress == "176.9.4.111" ) {
      Some(
        "Black listed, please respect robots.txt, see https://en.wikipedia.org/wiki/Robots_exclusion_standard")
    } else
      None
  }
}

trait SemanticControllerWrapper {
  def filterRequest(request: HTTPrequest, controller: SemanticController, filter: HTTPFilter): SemanticController =
    filter.filter(request) match {
      case None => controller
      case Some(message) => new SemanticController {
        override val featureURI = controller.featureURI
        override def result(request: HTTPrequest) = Text(message)
      }}

  def filterRequestResult(
    request:    HTTPrequest,
    controller: () => NodeSeq, filter: HTTPFilter): NodeSeq =
    filter.filter(request) match {
      case None          => controller()
      case Some(message) => Text(message)
    }
}
