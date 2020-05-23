package controllers

import java.io.{ByteArrayInputStream, OutputStream}

import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results._

import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.BrowsableGraph
import akka.stream.scaladsl.StreamConverters
import deductions.runtime.services.RDFContentNegociation
import deductions.runtime.jena.RDFStoreLocalJenaProvider

class DownloadServiceApp extends  {
  override implicit val config = new PlayDefaultConfiguration
} with RDFStoreLocalJenaProvider
with DownloadService

trait DownloadService extends HTTPrequestHelpers
  with BrowsableGraph[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with RDFContentNegociation {
  /**
   * get RDF with content negotiation (conneg) for RDF syntax;
   *  see also LDP.scala
   *
   *  cf https://www.playframework.com/documentation/2.3.x/ScalaStream
   */
  def downloadAction(url: String, database: String = "TDB") =
    Action {
      implicit request: Request[_] =>
        val httpRequest = copyRequest(request)
        def output(mime: String): Result = {
//          logger.debug(log("downloadAction", request))
          Ok.chunked{
            // TODO >>>>>>> add database arg.
            download(url, mime)
          } . as(s"${mime}; charset=utf-8")
            . withHeaders("Access-Control-Allow-Origin" -> "*")
        }

        val accepts = httpRequest.getHTTPheaderValue("Accept")
        val mime = computeMIMEOption(accepts)

        val syntaxOption = httpRequest.getHTTPparameterValue("syntax")
//        logger.debug((s">>>>>>>> downloadAction syntaxOption $syntaxOption"))
        syntaxOption match {
          case Some(syntax) =>
            val mimeOption = stringMatchesRDFsyntax(syntax)
//            logger.debug((s">>>>>>>> downloadAction , mimeOption $mimeOption"))
            mimeOption match {
              case Some(mimeStringFromSyntaxHTTPparameter) =>
                output(mimeStringFromSyntaxHTTPparameter)
              case None =>
                output(mime)
            }
          case None =>
            output(mime)
        }
    }

  /** implements download of RDF content from HTTP client;
   *  TODO should be non-blocking !!!!!!!!!!!!!
   *  currently accumulates a string first !!!
   *  not sure if Banana and Jena allow a non-blocking access to SPARQL query results */
  def download(url: String, mime: String="text/turtle") = {
	  val res = downloadAsString(url, mime)
	  val input = new ByteArrayInputStream(res.getBytes("utf-8"))
	  StreamConverters.fromInputStream(() ⇒ input)
  }

  def downloadAsString(url: String, mime: String="text/turtle"): String = {
    logger.info( s"download url $url mime $mime")
    val res = focusOnURI(url, mime)
    logger.info(s"""download result "$res" """)
    res
  }


  /** not working !!!!!!!!!!!!!  */
//  private def downloadKO(url: String): Enumerator[Array[Byte]] = {
//    // cf https://www.playframework.com/documentation/2.3.x/ScalaStream
//    // and http://greweb.me/2012/11/play-framework-enumerator-outputstream/
//    Enumerator.outputStream { os =>
//      val graph = search_only(url)
//      logger.info(s"after search_only($url)")
//      val r = graph.map { graph =>
//        /* non blocking */
//        val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
//        logger.info("before writer.write()")
//        val ret = writer.write(graph, os, base = url)
//        logger.info("after writer.write()")
//        os.close()
//      }
//      logger.info("after graph.map()")
//    }
//  }
}