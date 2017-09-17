package controllers

import scala.util.Failure
import scala.util.Success

import play.api.http.MediaRange
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import play.api.mvc.Result
import deductions.runtime.services.LoadService
import deductions.runtime.jena.ImplementationSettings

/** SPARQL compliant services: SPARQL query, SPARQL update, SPARQL load */
trait SparqlServices extends ApplicationTrait
    with LoadService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
{
  import config._

  /** load RDF String in database - TODO conneg !!! */
  def loadAction() = Action {
    implicit request: Request[AnyContent] =>
      val requestCopy = getRequestCopy()
      println(s"body class ${request.getClass} ${request.body} - data ${request.getQueryString("data")} ")     
      val content = request.getQueryString("data") match {
        case Some(s) => Some(s)
        case None => getContent(request)
      }
      println(s"content ${content.toString.substring(0, Math.min(content.toString.length,50)) + " ..."}")
      load(requestCopy.copy(content = content))
      Ok("OK")
  }

    /**
   * SPARQL GET compliant, construct or select
   * conneg => RDF/XML, Turtle or json-ld
   * 
   * TODO rename sparqlService
   */
  def sparqlConstruct(query: String) =
        Action {
//    withUser {
//      implicit userid =>
        implicit request: Request[_] =>
          logger.info(s"""sparqlConstruct: sparql: request $request
            sparql: $query
            accepts ${request.acceptedTypes} """)
          val lang = chooseLanguage(request)

          // TODO better try a parse of the query
          def checkSPARQLqueryType(query: String) =
            if (query.toLowerCase().contains("select") )
              "select"
            else
              "construct"

          val isSelect = (checkSPARQLqueryType(query) == "select")
          
          outputSPARQL(query, request.acceptedTypes, isSelect)
//          renderResult(output, default = mime)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
          /* access-control-allow-headers :"Accept, Authorization, Slug, Link, Origin, Content-type, 
           * DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,
           * If-Modified-Since,Cache-Control,Content-Type,Accept-Encoding"
           */
  }

  /**
   * SPARQL POST compliant, construct or select SPARQL query
   *  conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlConstructPOST = Action {
    implicit request: Request[AnyContent] =>
      logger.info(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val body: AnyContent = request.body

      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded
      val r = formBody.map { map =>

        val query0 = map.getOrElse("query", Seq())
        val query = query0 . mkString("\n")
        logger.info(s"""sparql: $query""" )

        // TODO better try a parse of the query
        def checkSPARQLqueryType(query: String) =
          if (query.toLowerCase().contains("select") )
            "select"
          else
            "construct"
        val isSelect = (checkSPARQLqueryType(query) == "select")
        val acceptedTypes = request.acceptedTypes

        outputSPARQL(query, acceptedTypes, isSelect)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      }
      r match {
        case Some(r) => r
        case None => BadRequest("BadRequest")
      }
  }
  
  def updateGET(updateQuery: String): EssentialAction = update(updateQuery)

  def updatePOST(): EssentialAction = update("")
  
  /** output SPARQL query as Play! Result;
   *  priority to accepted MIME type
   *  @param acceptedTypes from Accept HTTP header
   *  TODO move to Play! independant trait */
  protected def outputSPARQL(query: String, acceptedTypes: Seq[MediaRange], isSelect: Boolean): Result = {
    val preferredMedia = acceptedTypes.map { media => Accepting(media.toString()) }.headOption
    val defaultMIMEaPriori = if (isSelect) AcceptsSPARQLresults else AcceptsJSONLD
    val defaultMIME = preferredMedia.getOrElse(defaultMIMEaPriori)

    // TODO implicit class ResultFormat(val format: String)
    val resultFormat: String = mimeAbbrevs.getOrElse(defaultMIME, 
        mimeAbbrevs.get( defaultMIMEaPriori) . get )
    logger.info(s"""sparqlConstruct: output(accepts=$acceptedTypes) => result format: "$resultFormat" """)

    if (preferredMedia.isDefined &&
      !mimeSet.contains(preferredMedia.get))
      logger.info(s"CAUTION: preferredMedia $preferredMedia not in this application's list: ${mimeAbbrevs.keys.mkString(", ")}")

    val result = if (isSelect)
      sparqlSelectConneg(query, resultFormat, dataset)
    else
      sparqlConstructResult(query,
          // TODO
          lang="en",
          resultFormat)

    logger.info(s"result 5 first lines: $result".split("\n").take(5).mkString("\n"))
    Ok(result)
      .as(s"${simpleString2mimeMap.getOrElse(resultFormat, defaultMIMEaPriori).mimeType }")
    // charset=utf-8" ?
  }

  protected def update(update: String) =
    withUser {
      implicit userid =>
        implicit request =>
          logger.info("sparql update: " + request)
          logger.info(s"sparql: update '$update'")
          println(log("update", request))
          val update2 =
            if (update == "") {
              println(s"""contentType ${request.contentType}
                ${request.mediaType}
                ${request.body.getClass}
            """)
              val bodyAsText = request.body.asText.getOrElse("")
              if (bodyAsText != "")
                bodyAsText
              else
                request.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("query", Seq("")).headOption.getOrElse("")
            } else update
          logger.info(s"sparql: update2 '$update2'")
          val lang = chooseLanguage(request) // for logging
          val res = wrapInTransaction( sparqlUpdateQuery(update2) ) .flatten
          res match {
            case Success(s) => Ok(s"$res")
            case Failure(f) =>
              logger.error(res.toString())
              BadRequest(f.toString())
          }
    }

}
