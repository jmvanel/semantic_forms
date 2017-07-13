package controllers


import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.services.{ApplicationFacadeImpl, CORS}
import deductions.runtime.utils.{Configuration, HTTPrequest, RDFPrefixes, URIManagement}
import play.api.http.MediaRange
import play.api.mvc._
import views.MainXmlWithHead

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq
import java.io.File

import deductions.runtime.abstract_syntax.{FormSyntaxFactory, FormSyntaxFromSPARQL}
import deductions.runtime.html.TableView
import deductions.runtime.services.html.HTML5TypesTrait
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.LoadService
import deductions.runtime.services.html.{Form2HTMLBanana, HTML5TypesTrait}
import deductions.runtime.utils.DefaultConfiguration
import play.api.mvc.{AnyContentAsRaw, AnyContentAsText, RawBuffer}

//object Global extends GlobalSettings with Results {
//  override def onBadRequest(request: RequestHeader, error: String) = {
//    Future{ BadRequest("""Bad Request: "$error" """) }
//  }
//}

/** main controller 
 *  TODO split HTML pages & HTTP services */
trait ApplicationTrait extends Controller
    with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LanguageManagement
    with Secured
    with MainXmlWithHead
    with CORS
    with HTTPrequestHelpers
    with RDFPrefixes[ImplementationSettings.Rdf]
    with URIManagement
    with RequestUtils
    with LoadService[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormSyntaxFactory[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormSyntaxFromSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJenaProvider
{

	val config: Configuration

	implicit val myCustomCharset = Codec.javaSupported("utf-8")

  protected def tableFromSPARQL(request: HTTPrequest): NodeSeq = {
    val query = request.queryString.getOrElse("query", Seq()).headOption.getOrElse("")
    val formSyntax = createFormFromSPARQL(query,
      editable = false,
      formuri = "")
    val tv = new TableView[ImplementationSettings.Rdf#Node, ImplementationSettings.Rdf#URI]
        with Form2HTMLBanana[ImplementationSettings.Rdf]
        with ImplementationSettings.RDFModule
        with HTML5TypesTrait[ImplementationSettings.Rdf] {
      val config = new DefaultConfiguration {}
      val nullURI = ops.URI("")
    }
    tv.generate(formSyntax)
  }
  protected def makeJSONResult(json: String) = makeResultMimeType(json, AcceptsJSONLD.mimeType)

  protected def makeResultMimeType(content: String, mimeType: String) =
    Ok(content) .
         as( AcceptsJSONLD.mimeType + "; charset=" + myCustomCharset.charset )
         .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")

    
  /** generate a Main Page wrapping given XHTML content */
  protected def outputMainPage( content: NodeSeq,
      lang: String, userInfo: NodeSeq = <div/>, title: String = "", displaySearch:Boolean = true,
      classForContent: String = "container sf-complete-form"
	)
  (implicit request: Request[_]) = {
      Ok( "<!DOCTYPE html>\n" +
        mainPage( content, userInfo, lang, title, displaySearch,
            messages = getDefaultAppMessage(),
            classForContent )
      ).withHeaders("Access-Control-Allow-Origin" -> "*") // for dbpedia lookup
      .as("text/html; charset=utf-8")
  }

  protected def getDefaultAppMessage(): NodeSeq = {
    val messagesFile = "messages.html"
    if (new File(messagesFile).exists()) {
      try { scala.xml.XML.loadFile(messagesFile) }
      catch { case e: Exception => <p>Exception in reading message file: { e }</p> }
    } else
      <p>...</p>
  }
  /////////////////////////////////

  /** save Only, no display */
  protected def saveOnly(request: Request[_], userid: String, graphURI: String = ""): (String, Boolean) = {
    val body = request.body
    val host  = request.host
    body match {
      case form: AnyContentAsFormUrlEncoded =>
        val lang = chooseLanguage(request)
        val map = form.data
        logger.debug(s"ApplicationTrait.saveOnly: ${body.getClass}, map $map")
        // cf http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
        val subjectUriTryOption = Try {
          saveForm(map, lang, userid, graphURI, host)
        }
        subjectUriTryOption match {
            case Success((Some(url1), typeChange)) => (url1, typeChange)
            case _ => ("", false)
        }
    }
  }

  protected val AcceptsTTL = Accepting("text/turtle")
  protected val AcceptsJSONLD = Accepting("application/ld+json")
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  protected val AcceptsSPARQLresults = Accepting("application/sparql-results+json")

  protected val turtle = AcceptsTTL.mimeType

	// format = "turtle" or "rdfxml" or "jsonld"
	val mimeAbbrevs = Map( AcceptsTTL -> "turtle", AcceptsJSONLD -> "jsonld", AcceptsRDFXML -> "rdfxml",
	    Accepts.Json -> "json", Accepts.Xml -> "xml", AcceptsSPARQLresults -> "json" )

	 val simpleString2mimeMap = mimeAbbrevs.map(_.swap)

  protected def renderResult(output: Accepting => Result, default: Accepting = AcceptsTTL)(implicit request: RequestHeader): Result = {
    render {
      case AcceptsTTL()    => output(AcceptsTTL)
      case AcceptsJSONLD() => output(AcceptsJSONLD)
      case AcceptsRDFXML() => output(AcceptsRDFXML)
      case Accepts.Json()  => output(Accepts.Json)
      case Accepts.Xml()   => output(Accepts.Xml)
      case AcceptsSPARQLresults() => output(AcceptsSPARQLresults)
      case _             => output(default)
    }
  }

  val mimeSet = mimeAbbrevs.keys.toSet
//     Set(AcceptsTTL, AcceptsJSONLD, AcceptsRDFXML, Accepts.Json, Accepts.Xml)
  protected def computeMIME(accepts: Accepting, default: Accepting): Accepting = {
    if( mimeSet.contains(accepts))
       accepts
    else default
  }

  protected def computeMIME(accepts: Seq[MediaRange], default: Accepting): Accepting = {
    val v = accepts.find {
      mediaRange => val acc = Accepting(mediaRange.toString())
      mimeSet.contains(acc) }
    v match {
      case Some(acc) => Accepting(acc.toString())
      case None => default
    }
  }

  protected def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("") . trim()
  }

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
      sparqlConstructResult(query, resultFormat)

    logger.info(s"result $result".split("\n").take(5).mkString("\n"))
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
              if( bodyAsText != "" )
                bodyAsText
              else
                request.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("query", Seq("")).headOption.getOrElse("")
            } else update
          logger.info(s"sparql: update2 '$update2'")
          val lang = chooseLanguage(request) // for logging
          val res = sparqlUpdateQuery(update2)
          res match {
            case Success(s) => Ok(s"$res")
            case Failure(f) =>
              logger.error(res.toString())
              BadRequest(f.toString())
          }
    }

  protected def getContent(request: Request[AnyContent]): Option[String] = {
    request.body match {
      case AnyContentAsText(t) => Some(t)
      case AnyContentAsFormUrlEncoded(m) =>
        println(s"getContent 1 request.body AnyContentAsFormUrlEncoded size ${m.size}")
        m.keySet.headOption
      case AnyContentAsRaw(raw: RawBuffer) =>
        println(s"getContent 2 request.body.asRaw ${raw}")
        raw.asBytes(raw.size.toInt).map {
          arr => new String(arr.toArray, "UTF-8")
        }
      case _ => None
    }
  }
}
