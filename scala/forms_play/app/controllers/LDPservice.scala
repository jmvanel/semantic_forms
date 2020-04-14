package controllers

import java.net.URI
import java.net.URLDecoder

import scalaz._
import Scalaz._

import scala.util.Success

import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.AnyContent

import org.apache.commons.codec.digest.DigestUtils

class LDPservicesApp extends  {
    override implicit val config = new PlayDefaultConfiguration
  }
  with LDPservice
  with HTMLGenerator // TODO: should not be needed!

trait LDPservice extends ApplicationTrait {

  /** */
  def ldpPOSTAction(uri: String = "") =
//    withUser {
//      implicit userid =>
//        implicit request =>
        Action { implicit request: Request[AnyContent] =>
          logger.info("\nLDP: " + request)
          val slug = request.headers.get("Slug")
          val link = request.headers.get("Link")
          val contentType = request.contentType
          val content = getContent(request)
          logger.info(s"LDP POST: slug: $slug, link $link")
          logger.info(s"	LDP POST: content: $content")
          logger.info(s"	LDP POST: headers: ${request.headers}")
          val copiedRequest = copyRequest(request)
          val serviceCalled =
            ldpPOST(uri, link, contentType, slug, content, copiedRequest ).getOrElse("default")
          val result = Created("").as("text/plain; charset=utf-8")
            .withHeaders(
//              ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
                "Location" -> serviceCalled,
                "Link" -> """<http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/ns/ldp#Resource>; rel="type""""
                )
          logger.info(s"	LDP POST: $result")
          result
  }

  def ldpPOSTActionNoURI() = ldpPOSTAction()

  def ldpDeleteResource(uri: String) =
    Action { implicit request: Request[AnyContent] =>
      logger.info("\nLDP DELETE: " + request)
      val httpRequest = copyRequest(request)
      deleteResource(uri, httpRequest) match {
        case Success(s) =>
          NoContent.as("text/plain; charset=utf-8")
            .withHeaders("Link" -> """<http://www.w3.org/ns/ldp#Resource>; rel="type"""")
        case scala.util.Failure(f) =>
          InternalServerError(f.toString())
      }
    }

  def ldpHEAD(uri: String) =
    Action { implicit request: Request[AnyContent] =>
      logger.info("\nLDP: HEAD: " + request)
      val httpRequest = copyRequest(request)
      Ok("")
      // NOTE: this does not work, see:
      // https://www.playframework.com/documentation/2.6.x/ScalaResults#Changing-the-default-Content-Type
      // .withHeaders( CONTENT_TYPE -> defaultContentType )
      // TODO, ETag like GET <<<<<<<<<<<<
      .withHeaders("ETag" -> "123456789") // """"${DigestUtils.md5Hex(response)}"""" )
       .withHeaders(defaultLDPheaders : _* )
       .as(defaultContentType)
    }
}