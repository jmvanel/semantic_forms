package controllers

import java.io.File

import scala.io.Source
import scala.xml.NodeSeq

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.ServiceListenersManager
import deductions.runtime.utils.I18NMessages
import deductions.runtime.sparql_cache.SPARQLHelpers

import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results._

import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFStoreLocalProvider

import scalaz._

/** controller base;
 *  HTML pages & HTTP services are in WebPages and Services */
trait ApplicationUtils
    extends HTTPrequestHelpers
    with RDFContentNegociationPlay
    with deductions.runtime.utils.RDFStoreLocalProvider[org.w3.banana.jena.Jena,org.apache.jena.query.Dataset]
    with RDFStoreLocalJenaProvider
    with ServiceListenersManager[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RequestUtils {

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
