package controllers

import java.io.File

import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.CORS
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.RDFPrefixes

import play.api.mvc.Accepting
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result

import deductions.runtime.views.MainXmlWithHead
import deductions.runtime.core.HTTPrequest

import scala.io.Source

import play.api.http.HeaderNames
import play.api.mvc.Rendering

import scalaz._

//object Global extends GlobalSettings with Results {
//  override def onBadRequest(request: RequestHeader, error: String) = {
//    Future{ BadRequest("""Bad Request: "$error" """) }
//  }
//}
/*
object PlaySettings {
  type MyControllerBase = play.api.mvc.Controller
    // play.api.mvc.BaseController
}
*/
/** controller base;
 *  HTML pages & HTTP services are in WebPages and Services */
trait ApplicationTrait extends
     RDFStoreLocalJenaProvider
with Rendering
    with HeaderNames
    with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with LanguageManagement
    with Secured
    with MainXmlWithHead[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CORS
    with HTTPrequestHelpers
    with RDFPrefixes[ImplementationSettings.Rdf]
    with RequestUtils
    with RDFContentNegociationPlay
    with HTTPoutputFromThrowable[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  val config: Configuration

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
    implicit val rdfLocalProvider
//    : RDFStoreLocalProvider[Rdf, DATASET]
    = this
    callServiceListeners(request)(request.userId, rdfLocalProvider)
  }
              
  /////////////////////////////////

  /** save Only, no display - TODO move outside of forms_play */
  protected def saveOnly(
      httpRequest: HTTPrequest,
      userid: String, graphURI: String = ""): (String, Boolean) = {
    val host = httpRequest.host
    val lang = httpRequest.getLanguage
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
}
