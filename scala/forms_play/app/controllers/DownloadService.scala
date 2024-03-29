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
import deductions.runtime.utils.RDFContentNegociation
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.core.HTTPrequest

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import deductions.runtime.utils.StringHelpers
import deductions.runtime.core.IPFilter

class DownloadServiceApp @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration)
  extends {
    override implicit val config = new PlayDefaultConfiguration(configuration)
  }
  with AbstractController(components)
  with RDFStoreLocalJenaProvider
  with HTTPrequestHelpers
  with BrowsableGraph[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with RDFContentNegociation
  with StringHelpers {

  val filter = new IPFilter{}

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
        def output(mime: String, httpRequest: HTTPrequest): Result = {
//          logger.debug(log("downloadAction", request))
          Ok.chunked{
            // TODO >>>>>>> add database arg.
            val isBlanknode = httpRequest.getHTTPparameterValue("blanknode").getOrElse("") == "true"
//            println(s"downloadAction: isBlanknode $isBlanknode ; $httpRequest")
            val url1 = if(isBlanknode) "_:"+ url else url
            logger.info( s"${httpRequest.logRequest()} - download mime=$mime")
            filter.filter(httpRequest) match {
            case None          =>  download( url1, mime)
            case Some(message) => makeSourceFromString(message)
            }
          }.as(s"${mime}; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
            .withHeaders("Content-Disposition" ->
              ("filename=" +
                substringAfterLastIndexOf(url, "/").getOrElse("from-SF")
                + "." + mimeToExtension(mime)))
        }

        val accepts = httpRequest.getHTTPheaderValue("Accept")
        val mime = computeMIMEOption(accepts)

        val syntaxOption = httpRequest.getHTTPparameterValue("syntax")
        logger.debug((s">>>>>>>> downloadAction syntaxOption $syntaxOption"))
        syntaxOption match {
          case Some(syntax) =>
            val mimeOption = stringMatchesRDFsyntax(syntax)
            logger.debug((s">>>>>>>> downloadAction , mimeOption $mimeOption"))
            mimeOption match {
              case Some(mimeStringFromSyntaxHTTPparameter) =>
                output(mimeStringFromSyntaxHTTPparameter, httpRequest)
              case None =>
                output(mime, httpRequest)
            }
          case None =>
            output(mime, httpRequest)
        }
    }

  /** implements download of RDF content from HTTP client;
   *  TODO should be non-blocking !!!!!!!!!!!!!
   *  currently accumulates a string first !!!
   *  not sure if Banana and Jena allow a non-blocking access to SPARQL query results
   *  TODO add arg. HTTPrequest to set base URI */
  def download(url: String, mime: String="text/turtle") = {
	  val res = downloadAsString(url, mime)
	  makeSourceFromString(res)
  }

  private def makeSourceFromString(res: String) = {
	  val input = new ByteArrayInputStream(res.getBytes("utf-8"))
          StreamConverters.fromInputStream(() => input)
  }

  /** TODO add arg. HTTPrequest to set base URI */
  def downloadAsString(url: String, mime: String="text/turtle"): String = {
    val res = focusOnURI(url, mime)
    logger.debug(s"""download result "$res" """)
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
