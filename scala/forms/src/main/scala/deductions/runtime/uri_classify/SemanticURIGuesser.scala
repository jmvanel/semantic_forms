package deductions.runtime.uri_classify

import java.net.URL
import java.net.URLConnection
import java.nio.file.Paths

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.http.model.HttpHeader
import akka.http.model.HttpMethods.HEAD
import akka.http.model.HttpResponse
import akka.http.model.MediaType
import deductions.runtime.sparql_cache.RDFCache

/**
 * classify URI (non-blocking): leveraging on MIME types in HTTP headers.
 * MIME categories :
 * text (including HTML),
 * image,
 * sound,
 * video,
 * xml, and other machine processable stuff
 * rdf, Json-LD, Turtle and other semantic content
 */
object SemanticURIGuesser extends RDFCache {

  sealed abstract class SemanticURIType {
    override def toString = getClass.getSimpleName
  }
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

  def makeSemanticURIType(s: String): SemanticURIType = {
    s match {
      case _ if (s.endsWith("SemanticURI")) => SemanticURI
      case _ if (s.endsWith("Application")) => Application
      case _ if (s.endsWith("Audio")) => Audio
      case _ if (s.endsWith("Image")) => Image
      case _ if (s.endsWith("Message")) => Message
      case _ if (s.endsWith("Multipart")) => Multipart
      case _ if (s.endsWith("Text")) => Text
      case _ if (s.endsWith("Video")) => Video
      case _ if (s.endsWith("Data")) => Data
      case _ => Unknown
    }
  }
  def guessSemanticURIType(url: String): Future[SemanticURIType] = {
    if (isGraphInUse(url)) Future.successful(SemanticURI)
    else {
      val response: Future[HttpResponse] = HttpClient.makeRequest(url, HEAD)
      println(s"response $url " + response)
      response.map { resp => semanticURITypeFromHeaders(resp, url) }
    }
  }

  private def semanticURITypeFromHeaders(
    resp: HttpResponse,
    url: String): SemanticURIType = {
    val headers: Seq[HttpHeader] = resp.headers
    println(s"HttpResponse headers ${headers.mkString("\t\n")}")
    type ContentType = akka.http.model.headers.`Content-Type`
    val contentType = resp.header[ContentType]
    println("semanticURITypeFromHeaders: contentType " + contentType)
    val semanticURIType: SemanticURIType =
      contentType match {
        case Some(header) =>
          println("header " + header)
          val mediaType = header.contentType.mediaType
          val semanticURIType: SemanticURIType = makeSemanticURIType(mediaType, url)
          if (semanticURIType == Unknown)
            makeSemanticURITypeFromSuffix(url)
          else semanticURIType
        case None => makeSemanticURITypeFromSuffix(url)
      }
    println(s"semanticURITypeFromHeaders: semanticURIType $url " + semanticURIType)
    semanticURIType
  }

  private def makeSemanticURITypeFromSuffix(url: String): SemanticURIType = {
    val mimeTypeFromSuffix = trySuffix(url)
    println("makeSemanticURITypeFromSuffix: mimeTypeFromSuffix " + mimeTypeFromSuffix)
    if (mimeTypeFromSuffix == null)
      Unknown
    else {
      val mediaTypeFromSuffix = MediaType.custom(mimeTypeFromSuffix)
      println("makeSemanticURITypeFromSuffix: mediaTypeFromSuffix " + mediaTypeFromSuffix)
      makeSemanticURIType(mediaTypeFromSuffix, url)
    }
  }

  private def makeSemanticURIType(mt: MediaType, url: String) = {
    mt match {
      case mt if mt.isApplication => guessFromHeader(mt, url, Application)
      case mt if mt.isAudio => Audio
      case mt if mt.isImage => Image
      case mt if mt.isMessage => Data
      case mt if mt.isMultipart => Data
      case mt if mt.isText => guessFromHeader(mt, url, Text)
      case mt if mt.isVideo => Video
      case _ => Unknown
    }
  }

  /**
   * take in account suffix (jpg, etc) ;
   *  TODO: works in the REPL, but not in TestSemanticURIGuesser
   */
  private def trySuffix(url: String): String = {
    import java.nio.file._
    import java.net._
    val urlObj = new URL(url)
    println("trySuffix " + url)
    // to eliminate #me in FOAF profiles: 
    val source = Paths.get(urlObj.getFile)
    val source2 = source.toString()

    val mimeType = URLConnection.guessContentTypeFromName(url)
    val mimeType2 = mimeType match {
      case null => source2 match {
        // TODO : do not use strings but an API
        case _ if (source2.endsWith(".rdf")) => "application/rdf+xml"
        case _ if (source2.endsWith(".owl")) => "application/owl+xml"
        case _ if (source2.endsWith(".ttl")) => "application/turtle"
        case _ if (source2.endsWith(".n3")) => "application/n3"
        case _ => "application/text"
      }
      case _ => mimeType
    }
    //    val res = Files.probeContentType(source)
    println(s"trySuffix  + $mimeType + $mimeType2")
    mimeType2
  }

  private def guessFromHeader(mt: MediaType, url: String, t: SemanticURIType): SemanticURIType = {
    val st = mt.subType
    if (st == "rdf+xml" ||
      st == "turtle" ||
      st == "n3" ||
      st == "x-turtle") {
      SemanticURI
    } else t
  }

}