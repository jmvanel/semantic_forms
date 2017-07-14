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
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.AnyContentAsRaw
import play.api.mvc.AnyContentAsText
import play.api.mvc.Codec
import play.api.mvc.Controller
import play.api.mvc.RawBuffer
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import views.MainXmlWithHead

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
