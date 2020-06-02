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

import deductions.runtime.core.HTTPrequest
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.LoadService

import scalaz._
import Scalaz._
import play.api.mvc.RequestHeader

//import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
//import deductions.runtime.sparql_cache.algos.GraphEnrichment

/** SPARQL compliant services: SPARQL query, SPARQL update, SPARQL load */
trait SparqlServices extends ApplicationTrait
    with LoadService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
//    with GraphEnrichment[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
{
  import config._


  /** load RDF String in database, cf
   *  https://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/#http-post
   *  The file size is limited to 8Mb ;
   *  for larger files, use locally RDFLoaderApp or RDFLoaderGraphApp .
   */
  def loadAction() = Action( parse.anyContent(maxLength = Some((1024 * 1024 * 8 ).longValue) )) {
    implicit request: Request[AnyContent] =>
      val requestCopy = getRequestCopyAnyContent()
      logger.info(s"""body class ${request.getClass} request.body ${request.body.getClass}
      - data= "${request.getQueryString("data")}" """)
      val content = request.getQueryString("data") match {
        case Some(s) => Some(s)
        case None => getContent(request)
      }
      logger.info(s"content ${content.toString.substring(0, Math.min(content.toString.length,50)) + " ..."}")
      val resultGraph = load(requestCopy.copy(content = content))
      resultGraph match {
        case Success(g) => Ok("OK")
        case Failure(f) =>
          var errorMessage = f.getLocalizedMessage
          if(errorMessage . contains("Request Entity Too Large"))
            errorMessage += """The file size is limited to 8Mb ( in loadAction() ).
For larger files, use locally RDFLoaderApp or RDFLoaderGraphApp"""
          InternalServerError(errorMessage)
      }
  }


  /** sparql compliant GET Service, Construct or SELECT */
  def sparqlGET(query: String): Action[AnyContent] =
    sparqlConstructParams(query)

  /**
   * SPARQL GET compliant, construct or select,
   * with Union Graph;
   * conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlGetUnionGraph(query: String ): Action[AnyContent] =
    sparqlConstructParams(query,
        context=Map("unionDefaultGraph" -> "true"))

  /** For Sparql GET Service, Construct or SELECT, with or without Union Graph */
  private def sparqlConstructParams(query: String,
      bindings: Map[String,String] = Map(),
      context: Map[String,String] = Map()): Action[AnyContent] =
        Action {
        implicit request: Request[AnyContent] =>
          logInfo(s"""sparqlConstruct: sparql: request $request
            sparql: $query
            accepts ${request.acceptedTypes} """)

      val httpRequest = copyRequest(request)
      val accepts = httpRequest.getHTTPheaderValue("Accept")
      val firstMimeTypeAccepted = accepts.getOrElse("").replaceFirst(",.*", "")
      logger.debug( "firstMimeTypeAccepted " + firstMimeTypeAccepted )
      if (firstMimeTypeAccepted == "text/html") {
        val call0 = routes.WebPagesApp.sparql(query)
        val call = play.api.mvc.Call(method="GET",
            call0.url + "&label=" + httpRequest.getHTTPparameterValue("label").getOrElse("") )
        Redirect(call).flashing(
         "message" -> "Redirect to SPARQL UI" )
      } else {
          val isSelect = (checkSPARQLqueryType(query) === "select")
          outputSPARQL(query, request.acceptedTypes, isSelect, bindings, context,
                       getRequestCopy() )
      }
  }

  /**
   * SPARQL POST compliant, construct or select SPARQL query
   *  conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlPOST =
    sparqlConstructPOSTimpl()

  /** sparql POST Service, Construct or SELECT */
  def sparqlPOSTUnionGraph =
    sparqlConstructPOSTimpl(context=Map("unionDefaultGraph" -> "true"))

  private def sparqlConstructPOSTimpl(
      context: Map[String,String] = Map()) = Action {
    implicit request: Request[AnyContent] =>
      logInfo(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)

      val body: AnyContent = request.body
      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded

      val r = formBody.map { map =>
        val query0 = map.getOrElse("query", Seq())
        val query = query0 . mkString("\n")
        logInfo(s"""sparql as FormUrlEncoded: $query""" )
        makeSPARQLoutput(query, request, context)
      }
      r match {
        case Some(r) => r
        case None =>
          reportErrorInSparql( request )
      }
  }

  private def makeSPARQLoutput(query: String, request: Request[AnyContent],
      context: Map[String,String] = Map() ): Result = {
        val isSelect = (checkSPARQLqueryType(query) === "select")
        val acceptedTypes = request.acceptedTypes
        outputSPARQL(query, acceptedTypes, isSelect, context=context,
            httpRequest = getRequestCopy()(request) )
  }

  /** TODO better try a parse of the query */
  private def checkSPARQLqueryType(query: String) =
            if (query.toLowerCase().contains("select") )
              "select"
            else
              "construct"

  private def reportErrorInSparql(request: Request[AnyContent] ) =
            getContent(request) match {
            case Some(query) =>
              logInfo(s"""sparql (several content types tested) query:
                $query""" )
              makeSPARQLoutput(query, request)
            case None =>
              val body: AnyContent = request.body           
              BadRequest(s"""BadRequest: body not recognized:
              body.asText
              ${body.asText}
              body class ${body.getClass}
              """)
  }

  def updateGET(updateQuery: String): EssentialAction = update(updateQuery)

  def updatePOST(): EssentialAction = update("")

  /** output SPARQL query as Play! Result;
   *  priority to accepted MIME type
   *  @param acceptedTypes from Accept HTTP header
   *  TODO move to Play! independant trait */
  private def outputSPARQL(query: String, acceptedTypes: Seq[MediaRange], isSelect: Boolean,
      params: Map[String,String] = Map(),
      context: Map[String,String] = Map(),
      httpRequest: HTTPrequest): Result =
    recoverFromOutOfMemoryErrorResult {
      val preferredMedia = acceptedTypes.map { media => Accepting(media.toString()) }.headOption
      val defaultMIMEaPriori = if (isSelect) AcceptsSPARQLresults else AcceptsJSONLD
      val defaultMIME = preferredMedia.getOrElse(defaultMIMEaPriori)

      // TODO implicit class ResultFormat(val format: String)
      val resultFormat: String = mimeAbbrevs.getOrElse(defaultMIME, 
        mimeAbbrevs.get( defaultMIMEaPriori) . get )
      logInfo(s"""outputSPARQL: output(accepts=$acceptedTypes) => result format: "$resultFormat"
        defaultMIMEaPriori $defaultMIMEaPriori
        preferredMedia $preferredMedia""")

    if (preferredMedia.isDefined &&
      !mimeSet.contains(preferredMedia.get))
      logInfo(s"""CAUTION: preferredMedia '$preferredMedia' not in this application's list:
        ${mimeAbbrevs.keys.mkString(", ")}""")

    val result = if (isSelect)
      sparqlSelectConneg(query, resultFormat, dataset, context)
    else {
      sparqlConstructResult(query,
          lang=httpRequest.getLanguage(),
          resultFormat,
          if( httpRequest.getHTTPparameterValue("enrich").isDefined )
            context + ("enrich" -> "true")
            else context
      )
    }
    logInfo(s"result 10 first lines: $result".split("\n").take(10).mkString("\n"))
    result match {
      case Success(result) =>
        Ok(result)
      .as(s"${simpleString2mimeMap.getOrElse(resultFormat, defaultMIMEaPriori).mimeType }")
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
      .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      /* access-control-allow-headers :"Accept, Authorization, Slug, Link, Origin, Content-type, 
       * DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,
       * If-Modified-Since,Cache-Control,Content-Type,Accept-Encoding" */
      // charset=utf-8" ?
      case Failure(f) => InternalServerError(f.getLocalizedMessage)
    }
  }

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
          logInfo(s"sparql: update2 '$update2'")
          val lang = chooseLanguage(request) // for logging
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
