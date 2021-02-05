package deductions.runtime.core

import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Node

import scala.io.Source
import scala.util.Try
import java.net.InetAddress
import scala.concurrent.Future

trait HTTPFilter {
  /** HTTP Filter
   *  @return a message for HTTP output if request is filtered out (typically blacklisted), or None */
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

  private lazy val noAccessByIP =
    ( blacklistedIPs contains "noAccessByIP" ) ||
    ( blacklistedIPs contains "no access by IP" )

  val responseToBlackListed = Some(
  "Black listed, please respect robots.txt, see https://en.wikipedia.org/wiki/Robots_exclusion_standard")

  private val regexIP = """\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""".r
  /** @return a message for HTTP output or None */
  override def filter(request: HTTPrequest): Option[String] = {
    val blacklistCriterium =
      ( blacklistedIPs contains request.remoteAddress ) ||
      // this excludes all access by IP : should be under configuration
      ( noAccessByIP &&
      regexIP.findAllIn(request.host).size == 1 )

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

  /** ensure that when filtered the controller is never called */
  def filterRequest(
      request: HTTPrequest,
      controller: SemanticController,
      filter: HTTPFilter): SemanticController = {
    new SemanticController {
      override val featureURI = controller.featureURI
      override def result(request: HTTPrequest) =
        filterRequestResult(
          request,
          () => controller.result(request),
          filter)
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def filterRequestFuture(request: HTTPrequest,
      controller: SemanticControllerFuture, filter: HTTPFilter): SemanticControllerFuture = {
    new SemanticControllerFuture {
      override val featureURI = controller.featureURI
      override def result(request: HTTPrequest) =
        filterRequestResult0(
          request,
          () => controller.result(request),
          filter) match {
            case Left(c) => c
            case Right(mess) => Future(mess)
          }
    }
  }

  def filterRequest2content(request: HTTPrequest, controller: SemanticController, filter: HTTPFilter): NodeSeq =
    filterRequest(request, controller, filter).result(request)
  def filterRequest2content(request: HTTPrequest,
      controller: SemanticControllerFuture, filter: HTTPFilter): Future[NodeSeq] =
    filterRequestFuture(request, controller, filter).result(request)

  private def filterRequestResult0[TYPE](
    request:    HTTPrequest,
    content: () => TYPE,
    filter: HTTPFilter): Either[TYPE, NodeSeq] =
    filter.filter(request) match {
      case None          => Left(content())
      case Some(message) => Right(Text(message))
    }

  import scala.xml.Text
  private def filterRequestResult(
    request:    HTTPrequest,
    content: () => NodeSeq, filter: HTTPFilter): NodeSeq =
    filter.filter(request) match {
      case None          => content()
      case Some(message) => Text(message)
    }
}
