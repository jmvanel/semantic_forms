package controllers

import java.io.File

import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq

import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.abstract_syntax.FormSyntaxFromSPARQL
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.CORS
import deductions.runtime.services.LoadService
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.URIManagement
import play.api.http.MediaRange
import play.api.mvc.Accepting
import play.api.mvc.AnyContent
import play.api.mvc.Codec
import play.api.mvc.RawBuffer
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import deductions.runtime.views.MainXmlWithHead
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFStoreLocalProvider
import scala.io.Source
import play.api.mvc.AnyContentAsXml
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Action
//import play.api.mvc.ControllerComponents

import scalaz._
import Scalaz._
import deductions.runtime.utils.I18NMessages

//object Global extends GlobalSettings with Results {
//  override def onBadRequest(request: RequestHeader, error: String) = {
//    Future{ BadRequest("""Bad Request: "$error" """) }
//  }
//}

object PlaySettings {
  type MyControllerBase = play.api.mvc.Controller
    // play.api.mvc.BaseController
}

/** controller base;
 *  HTML pages & HTTP services are in WebPages and Services */
trait ApplicationTrait extends PlaySettings.MyControllerBase
    with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LanguageManagement
    with Secured
    with MainXmlWithHead[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CORS
    with HTTPrequestHelpers
    with RDFPrefixes[ImplementationSettings.Rdf]
    with RequestUtils
    with RDFStoreLocalJenaProvider
{
	val config: Configuration

  implicit val myCustomCharset = Codec.javaSupported("utf-8")

  protected def makeJSONResult(json: String) = makeResultMimeType(json, AcceptsJSONLD.mimeType)

  protected def makeResultMimeType(content: String, mimeType: String) =
    Ok(content) .
         as( AcceptsJSONLD.mimeType + "; charset=" + myCustomCharset.charset )
         .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")

  protected def getDefaultAppMessage(): NodeSeq = readXHTMLFile( "messages.html" )
  protected def getDefaultHeadExtra():  NodeSeq = readXHTMLFile( "head-extra.html", <meta/>)

  private def readXHTMLFile(messagesFile: String, default: NodeSeq=(<p>...</p>)): NodeSeq = {
    if (new File(messagesFile).exists()) {
      try {
        scala.xml.Unparsed(
          Source.fromFile(messagesFile, "UTF-8").getLines().mkString("\n"))
      }
      catch { case e: Exception => <p>Exception in reading message file: { e }</p> }
    } else
      <p>...</p>
  }

  /** call All Service Listeners */
  protected def callAllServiceListenersPlay(request: Request[_]) = {
    val req = copyRequest(request)
    callAllServiceListeners(req)
  }

  protected def callAllServiceListeners(request: HTTPrequest) = {
    logger.debug( s">>>> callAllServiceListeners $request => ${request.uri}")
    implicit val rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET] = this
    callServiceListeners(request)(request.userId(), rdfLocalProvider)
  }
              
  /////////////////////////////////

  /** save Only, no display - TODO move outside of forms_play */
  protected def saveOnly(
      httpRequest: HTTPrequest,
      userid: String, graphURI: String = ""): (String, Boolean) = {
    val host = httpRequest.host
    val lang = httpRequest.getLanguage()
    val map = httpRequest.formMap
    logger.debug(
      s"""
        ApplicationTrait.saveOnly: request $httpRequest ,
        ApplicationTrait.saveOnly: map $map""")
    // cf http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
    val subjectUriTryOption = Try {
      saveForm(map, lang, userid, graphURI, host)
    }
    subjectUriTryOption match {
      case Success((Some(url1), typeChange)) => (url1, typeChange)
      case _                                 => ("", false)
    }
  }

  // TODO reuse trait RDFContentNegociation
  protected val AcceptsTTL = Accepting("text/turtle")
  protected val AcceptsJSONLD = Accepting("application/ld+json")
  protected val AcceptsRDFXML = Accepting("application/rdf+xml")
  protected val AcceptsSPARQLresults = Accepting("application/sparql-results+json")
  protected val AcceptsSPARQLresultsXML = Accepting("application/sparql-results+xml")
  protected val AcceptsICal = Accepting("text/calendar")

  protected val turtle = AcceptsTTL.mimeType

	/** mime Abbreviations, format = "turtle" or "rdfxml" or "jsonld" */
	val mimeAbbrevs = Map(
	    AcceptsTTL -> "turtle",
	    AcceptsJSONLD -> "jsonld",
	    AcceptsRDFXML -> "rdfxml",
	    Accepts.Json -> "json",
	    Accepts.Xml -> "xml",
	    AcceptsSPARQLresults -> "json",
	    AcceptsSPARQLresultsXML -> "xml",
	    AcceptsICal -> "ical"
	 )

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
    uriArgs.find { uri => uri  =/=  "" }.getOrElse("") . trim()
  }

  def recoverFromOutOfMemoryErrorResult(
    sourceCode: => Result,
    message:    String    = recoverFromOutOfMemoryErrorDefaultMessage("en") ): Result = {
    try {
      sourceCode
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        InternalServerError {
          <p>
            { message }
            <br/>
            { t.getLocalizedMessage }
          </p>
        }
    }
  }

  def errorResultFromThrowable(
    t:               Throwable,
    specificMessage: String    = "ERROR",
    request: Request[_] ): Result = {
    InternalServerError(
      s"""Error '$specificMessage', retry later !!!!!!!!
        ${request.uri}
          ${t.getLocalizedMessage}
          ${printMemory}""")
  }

    def errorActionFromThrowable(
    t:               Throwable,
    specificMessage: String    = "ERROR") = Action {
          implicit request: Request[_] =>
        errorResultFromThrowable(t, specificMessage, request)
    }

}
