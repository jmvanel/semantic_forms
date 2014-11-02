package deductions.runtime.uri_classify

import java.net.URL
import spray.http._
import spray.client.pipelining._
import akka.actor.ActorSystem
import scala.concurrent.Future
import spray.http.parser.HttpParser
import spray.http._
import parser.HttpParser
import CacheDirectives._
import HttpHeaders._
import MediaTypes._
import MediaRanges._
import HttpCharsets._
import HttpEncodings._
import HttpMethods._
import spray.util._

/* classify URI, leveraging on MIME types in HTTP headers.
MIME categories :
text (including HTML), 
image,
sound, 
video,
xml, and other machine processable stuff
rdf, Json-LD, Turtle and other semantic content 
 * */
object SemanticURIGuesser {
  
  abstract class SemanticURIType
  object SemanticURI extends SemanticURIType
  object Application extends SemanticURIType
  object Audio extends SemanticURIType
  object Image extends SemanticURIType
  object Message extends SemanticURIType
  object Multipart extends SemanticURIType
  object Text extends SemanticURIType
  object Video extends SemanticURIType
  object Data extends SemanticURIType
  object Unknown extends SemanticURIType
  
  def guessSemanticURIType(url: String) = {
    // cf http://spray.io/documentation/1.1-SNAPSHOT/spray-client/#usage
    println("before ActorSystem")
    implicit val system = ActorSystem("guessSemanticURIType")
    println("after ActorSystem")
    import system.dispatcher // execution context for futures
    println("system.dispatcher")
    
    // crashes here ::::::::::::::::::::::::::
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    println("sendReceive")
    val response: Future[HttpResponse] = pipeline(Head(url))
    println("response")
    response.map {
      resp => resp.status match {
          case StatusCodes.OK =>
            semanticURITypeFromHeaders(resp.headers, url)
          case _ => Unknown // or Future fails
        }
    }
  }

  private def semanticURITypeFromHeaders(headers: List[HttpHeader], url: String) : SemanticURIType = {
    val optionSemanticURIType : Option[SemanticURIType] = headers.collectFirst {
      case header if (header.is("content-type")) =>
        val semanticURIType: SemanticURIType = header match {
          case `Content-Type`(ct) =>
            val mediaType = ct.mediaType
            mediaType match {
              case mt if mt.isApplication => guessHeader(mt,url, Application)
              case mt if mt.isAudio => Audio
              case mt if mt.isImage => Image
              case mt if mt.isMessage => Data
              case mt if mt.isMultipart => Data
              case mt if mt.isText => guessHeader(mt,url, Text)
              case mt if mt.isVideo => Video
              case _ => Unknown
            }
          case _ => Unknown
        }
        semanticURIType
    }
    optionSemanticURIType match {
    case Some(semanticURIType) => semanticURIType
    case None => Unknown
    }
  }
  
  private def guessHeader(mt:MediaType, url: String, t:SemanticURIType) : SemanticURIType = {
    val st = mt.subType 
    if(
        st == "rdf+xml" || 
        st == "turtle" ||
        st == "n3" ||
        st == "x-turtle" ) {
      SemanticURI
    } else t
  }
  
//  HttpParser.parse( acceptHeaderTurtlePriority )
//  val acceptHeaderTurtlePriority =
//    "text/n3," +
//      "text/turtle;q=1, " +
//      "application/rdf+xml;q=0.6, " +
//      "text/xml;q=0.5" +
//      ", text/plain;q=0.4" +
//      ", text/xhtml;q=0.35" +
//      ", text/html;q=0.3"
//  val acceptHeaderRDFPriority =
//    "application/rdf+xml;q=1, " +
//      "text/xml;q=0.9" +
//      "text/n3;q=0.6," +
//      "text/turtle;;q=0.6, " +
//      ", text/plain;q=0.4" +
//      ", text/xhtml;q=0.35" +
//      ", text/html;q=0.3"
//      
  /* case class HttpResponse(status: StatusCode = StatusCodes.OK,
                        entity: HttpEntity = HttpEntity.Empty,
                        headers: List[HttpHeader] = Nil,
                        protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`) extends HttpMessage with HttpResponsePart {
   */
}
    //      case _ => println( "Not an Http URL Connection: " + url )
    //      }