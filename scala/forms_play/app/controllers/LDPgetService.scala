package controllers

import java.net.URI
import java.net.URLDecoder

import scalaz._
import Scalaz._

import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.AnyContent
import play.api.mvc.Results._
import play.api.http.HeaderNames._

import org.apache.commons.codec.digest.DigestUtils
import deductions.runtime.services.LDP
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJenaProvider

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

class LDPgetServicesApp @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration) extends  {
    override implicit val config = new PlayDefaultConfiguration
  }
  with AbstractController(components)
  with RDFStoreLocalJenaProvider
  // with LDPgetService
//trait LDPgetService extends
  with LDP[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with HTTPrequestHelpers {

  /** LDP GET
   *  @param uri0 relative URI, URL encoded */
  def ldp(uri0: String) = Action {
    implicit request: Request[_] =>
      val uri = {
        val uriObject = new URI(URLDecoder.decode(uri0, "UTF-8"))
        println( ">>>>>>>> ldp: " + uri0)
        println( ">>>>>>>> ldp: " + uriObject.getPath)
        // for Facebook
        uriObject.getPath
      }
      logger.info("LDP GET: request " + request)
      val acceptedTypes = request.acceptedTypes
      logger.info( s"acceptedTypes $acceptedTypes")

      val httpRequest = copyRequest(request)
      val accept = httpRequest.getHTTPheaderValue("Accept")
      val firstMimeTypeAccepted = accept.getOrElse("").replaceFirst(",.*", "")
      val mimeType =
        if( isKnownRdfSyntax(firstMimeTypeAccepted) ||
            firstMimeTypeAccepted === htmlMime )
          firstMimeTypeAccepted
        else
          jsonldMime

      logger.debug(s">>>> ldp($uri): mimeType $mimeType")
      if (mimeType  =/=  htmlMime) {
        val responseBody = getTriples(uri, request.path, mimeType, httpRequest)
        logger.info("LDP: GET: response Body\n" + responseBody)
        val contentType = mimeType + "; charset=utf-8"
        logger.info(s"contentType $contentType")
        Ok(responseBody)
          .as(contentType)
          .withHeaders("ETag" -> s""""${DigestUtils.md5Hex(responseBody)}"""" )
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          // TODO rather use timestamp on TDB2

          .withHeaders(defaultLDPheaders : _* )
//          .withHeaders("Link" -> """<http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/ns/ldp#Resource>; rel="type"""")
//          .withHeaders("Allow" -> "OPTIONS,GET,POST,PUT,PATCH,HEAD")
//          .withHeaders("Accept-Post" -> """"text/turtle, application/ld+json""")

      } else { //// Redirect to /display, without HTTP GET query (for Facebook)  ////
        val ldpURL = httpRequest.originalURLNoQuery()
        logger.debug(s">>>> ldp: Redirect to /display?displayuri= $ldpURL")
        val call = Redirect("/display", Map("displayuri" -> Seq(ldpURL)))
        call
      }
  }

}
