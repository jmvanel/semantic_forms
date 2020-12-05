package controllers

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import play.api.http.MediaRange
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result

import scalaz._
import Scalaz._

import deductions.runtime.core.HTTPrequest
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

/** SPARQL compliant services: SPARQL query */
class SparqlServicesGET @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration) extends {
    override implicit val config = new PlayDefaultConfiguration(configuration)
  }
with AbstractController(components)
with ApplicationUtils
/*
with HTTPrequestHelpers
    with RequestUtils
    with RDFContentNegociationPlay
    with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET] // for sparqlConstructResult
    with RecoverUtilities[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with StringHelpers
*/
{
  // import config._

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
      logInfo(s"""outputSPARQL: output(accepts=${acceptedTypes.mkString(", ")}) => result format: '$resultFormat'
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
    logInfo(s"result 10 first lines: $result".split("\n|\r\n").take(10).mkString("\n"))
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


  /** SPARQL result
   * @param format : "turtle" or "rdfxml" or "jsonld"
   */
  def sparqlConstructResult(query: String, lang: String,
      format: String = "turtle",
      context: Map[String,String] = Map()): Try[String] = {
    logger.info("Global.sparql query  " + query)
    if (query != "")
      sparqlConstructQueryTR(query, format,
          context + ("lang" -> lang))
//      match {
//        case Success(s) => Success(s)
//        case Failure(f) => Failure(f)
////          Failure( new Exception(
////            if( format === "turtle")
////              s"# $f" else "{error: \"" + f + "\" }" ))
//      }
    else  Success("# Empty query result !!!")
  }

  private def logInfo(s: String) = println(s) // logger.info(s)
}
