package controllers

import scala.util.Failure
import scala.util.Success

import play.api.mvc.AnyContent
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import play.api.mvc.Result

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.LoadService

import scalaz._
import Scalaz._

import deductions.runtime.services.RecoverUtilities
import deductions.runtime.utils.StringHelpers

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController


/** SPARQL compliant services: SPARQL update, SPARQL load */
class SparqlServices @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration) extends {
    override implicit val config = new PlayDefaultConfiguration(configuration)
  }
with AbstractController(components)
// with ApplicationTrait
with ApplicationUtils
    with Secured
    with LanguageManagement
    with HTTPoutputFromThrowable[ImplementationSettings.Rdf, ImplementationSettings.DATASET] 
    with LoadService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RecoverUtilities[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with StringHelpers
{
  // import config._

  /** load RDF String in database, cf
   *  https://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/#http-post
   *  The file size is limited to 8Mb ;
   *  for larger files, use locally RDFLoaderApp or RDFLoaderGraphApp,
   *  or client RDFuploader app.
   */
    def loadAction() =
    Action( parse.anyContent(maxLength = Some((1024 * 1024 * 8 ).longValue) )) {
    implicit request: Request[AnyContent] =>
      recoverFromOutOfMemoryErrorGeneric[Result](
          { // begin Result
            val requestCopy = getRequestCopyAnyContent()
      val message = requestCopy.getHTTPparameterValue("message")
      logger.info(s"""loadAction: before System.gc(): ${formatMemory()}""")
      System.gc()
      val messMemory = s"""loadAction: AFTER System.gc(): Free Memory: ${
        Runtime.getRuntime.freeMemory() / (1024 * 1024)} Mb"""
      logger.info(
          s"""loadAction: body class ${request.getClass} request.body ${request.body.getClass}
          message '$message'
          $messMemory""")
      val content = request.getQueryString("data") match {
        case Some(s) => Some(s)
        case None => getContent(request)
      }
      val contentAbbrev = substringSafe(content.toString, 100)
      logger.info(s"loadAction: content $contentAbbrev ...")
      val resultGraph = load(requestCopy.copy(content = content))
      resultGraph match {
        case Success(g) => Ok(s"""OK
          loaded content $contentAbbrev
        to graph URI <${requestCopy.getHTTPparameterValue("graph")}>
        message '$message',
        freeMemory ${
          val mb = 1024 * 1024 ;Runtime.getRuntime.freeMemory().toFloat / mb
          }""").as("text/plain")
        case Failure(f) =>
          val errorMessage = f.getMessage
          val comment = if(errorMessage != null && errorMessage . contains("Request Entity Too Large"))
            """ The file size is limited to 8Mb ( in loadAction() ).
                For larger files, use locally RDFLoaderApp or RDFLoaderGraphApp"""
          InternalServerError(
              errorMessage + "\n" +
              comment + "\n" +
              "message " + message +
              " , content: " +
              content.slice(0, 200))
      } // end resultGraph match
          } // end Result
    , // end arg 1 recoverFromOutOfMemoryErrorGeneric
      (t: Throwable) =>
        errorResultFromThrowable(t, "in /load", request)
    )
    } // end Action

//  /** For tests, send HTTP 500 InternalServerError */
//  def loadAction() =
//    Action( parse.anyContent(maxLength = Some((1024 * 1024 * 8 ).longValue) )) {
//    implicit request: Request[AnyContent] =>
//      logger.info(s"TEST: request $request")
//      InternalServerError(
//        s"""Error TEST!!!!, retry later !!!!!!!!
//        ${request.uri}
//        """)
//   }



  def updateGET(updateQuery: String): EssentialAction = update(updateQuery)

  def updatePOST(): EssentialAction = update("")


  private def update(update: String) =
    withUser {
      implicit userid =>
        implicit request =>
          logInfo("sparql update: " + request)
          logInfo(s"sparql: update '$update'")
          logInfo(log("update: request", request))
          val update2 =
            if (update === "") {
              logInfo(s"""update: contentType ${request.contentType}
                mediaType ${request.mediaType}
                request.body.getClass ${request.body.getClass}
              """)
              val bodyAsText = request.body.asText.getOrElse("")
              logger.debug(s"update: bodyAsText: '$bodyAsText'")
              if (bodyAsText  =/=  "")
                bodyAsText
              else {
                logger.debug(s"""update: request.body.asFormUrlEncoded :
                  '${request.body.asFormUrlEncoded}""")
                request.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("query", Seq("")).headOption.getOrElse("")
              }
            } else update
          logInfo(s"sparql: update2 '$update2' , userid '$userid'")
          val res = wrapInTransaction( sparqlUpdateQuery(update2) ) .flatten
          res match {
            case Success(s) =>
              Ok(s"$res")
                .withHeaders("Access-Control-Allow-Origin" -> "*") // for Spoggy, TODO something more secure!

            case Failure(f) =>
              logger.error(res.toString())
              BadRequest(f.toString())
          }
    }

  private def logInfo(s: String) = println(s) // logger.info(s)
}
