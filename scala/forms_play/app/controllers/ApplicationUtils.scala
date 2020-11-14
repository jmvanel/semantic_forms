package controllers

import java.io.File

import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq

import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.abstract_syntax.FormSyntaxFromSPARQL
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.services.CORS
import deductions.runtime.services.LoadService
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.URIManagement
import deductions.runtime.utils.ServiceListenersManager

import play.api.http.MediaRange
import play.api.mvc.Accepting
import play.api.mvc.AnyContent
//import play.api.mvc.Codec
import play.api.mvc.RawBuffer
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.BaseController
import play.api.mvc.Controller
import play.api.mvc.Results._

import deductions.runtime.views.MainXmlWithHead
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFStoreLocalProvider

import scala.io.Source

import play.api.mvc.AnyContentAsXml
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Action
//import play.api.mvc.ControllerComponents
import play.api.http.HeaderNames
import play.api.mvc.Rendering

import scalaz._
import Scalaz._
import deductions.runtime.utils.I18NMessages
import deductions.runtime.sparql_cache.SPARQLHelpers

/** controller base;
 *  HTML pages & HTTP services are in WebPages and Services */
trait ApplicationUtils
    extends HTTPrequestHelpers
    with RDFContentNegociationPlay
    with deductions.runtime.utils.RDFStoreLocalProvider[org.w3.banana.jena.Jena,org.apache.jena.query.Dataset]
    with RDFStoreLocalJenaProvider
    with ServiceListenersManager[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RequestUtils
/* extends
    Rendering
    with HeaderNames
    with LanguageManagement
    with Secured
    with MainXmlWithHead[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CORS
    with RDFPrefixes[ImplementationSettings.Rdf]
    with RDFStoreLocalJenaProvider
    with RDFContentNegociationPlay
    with HTTPoutputFromThrowable[ImplementationSettings.Rdf, ImplementationSettings.DATASET] 
*/
{

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
    : RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
//    : RDFStoreLocalProvider[Rdf, DATASET]
    = this
    callServiceListeners(request)(request.userId(), rdfLocalProvider)
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


  def recoverFromOutOfMemoryErrorDefaultMessage(lang: String) =
    I18NMessages.get("recoverFromOutOfMemoryErrorDefaultMessage", lang)
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
