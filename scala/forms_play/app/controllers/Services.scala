package controllers

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result

import scalaz._
import Scalaz._

import scala.util.Success
import scala.concurrent.Future

import org.apache.commons.codec.digest.DigestUtils

import deductions.runtime.services.RDFContentNegociation
import java.net.URI
import java.net.URLDecoder
import java.io.File


class ServicesApp extends  {
    override implicit val config = new PlayDefaultConfiguration
  }
  with Services
  with HTMLGenerator // TODO: why is it needed ?


/** controller for non-SPARQL Services (or SPARQL related but not in the W3C recommendations)
 *  including LDP */
trait Services extends ApplicationTrait
with RDFContentNegociation {

  /** /form-data service; like /form but raw JSON data */
  def formDataAction(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "", database: String = "TDB") =
    Action {
        implicit request: Request[_] =>
        val lang = chooseLanguage(request)
       makeJSONResult(
           formData(uri, blankNode, Edit, formuri, database, lang))
    }
  /**
   * creation form as raw JSON data
   *  TODO add database HTTP param.
   */
  def createData() =
    Action { implicit request: Request[_] =>
      logger.info("create: " + request)
      // URI of RDF class from which to create instance
      val classUri0 = getFirstNonEmptyInMap(request.queryString, "uri")
      val classUri = expandOrUnchanged(classUri0)
      // URI of form Specification
      val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
      logger.info(s"create: class URI <$classUri>")
      logger.info(s"create: formSpecURI from HTTP request: <$formSpecURI>")

      Ok(createDataAsJSON(classUri,
        formSpecURI,
        copyRequest(request))).
        as(AcceptsJSONLD.mimeType + "; charset=" + myCustomCharset.charset)
    }

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




  /**
   * service /sparql-data, like /form-data spits raw JSON data for a view,
   * but from a SPARQL CONSTRUCT query,
   *  cf issue https://github.com/jmvanel/semantic_forms/issues/115
   */
  def sparqlDataPOST = Action {
    // TODO pasted from sparqlConstructPOST
    implicit request: Request[AnyContent] =>
      logger.info(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val body: AnyContent = request.body

      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded
      val result = formBody.map { map =>

        val query0 = map.getOrElse("query", Seq())
        val query = query0.mkString("\n")
        logger.info(s"""sparql-data: query $query""")

        val Edit = map.getOrElse("Edit", Seq()).headOption.getOrElse("")
        val formuri = map.getOrElse("formuri", Seq()).headOption.getOrElse("")

        makeJSONResult(
          createJSONFormFromSPARQL(
            query,
            editable = (Edit  =/=  ""),
            formuri,
            copyRequest(request)))
      }

      result match {
        case Some(r) => r
        case None    => BadRequest(
          "sparqlDataPOST: BadRequest: nothing in form Body, and nothing in HTTP parameter query")
      }
  }

  def sparqlDataGET(sparqlQuery: String) = Action {
    implicit request: Request[AnyContent] =>
      val httpRequest = copyRequest(request)
      logger.info(
          s"""sparql-data GET: query $sparqlQuery""")
      val Edit = httpRequest.getHTTPparameterValue("Edit").getOrElse("")
      val formuri = httpRequest.getHTTPparameterValue("formuri").getOrElse("")
      makeJSONResult(
        createJSONFormFromSPARQL(
          sparqlQuery,
          editable = (Edit  =/=  ""),
          formuri,
          httpRequest))
  }


  //  implicit val myCustomCharset = Codec.javaSupported("utf-8") // does not seem to work :(

  def lookupService(search: String, clas: String = "") = {
    recoverFromOutOfMemoryErrorGeneric[Action[AnyContent]](
      {
      Action { implicit request: Request[AnyContent] =>
        logger.info(s"""Lookup: $request
            accepts ${request.acceptedTypes} """)
        val lang = chooseLanguage(request)
        val mime = request.acceptedTypes.headOption.map { typ => typ.toString() }.getOrElse(Accepts.Xml.mimeType)
        logger.info(s"mime $mime")
        Ok(lookup(search, lang, clas, mime)).as(s"$mime; charset=utf-8")
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      }
    },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in GET /lookup"))
  }

  def httpOptions(path: String) = {
	  Action { implicit request =>
      logger.info("\nLDP: OPTIONS: " + request)
//      Ok("OPTIONS: " + request)
      Ok("")
        .as("text/html; charset=utf-8")
        .withHeaders(corsHeaders.toList:_*)
    }
  }


  import scala.concurrent.ExecutionContext.Implicits.global

  /** load URI into TDB; HTTP param publisher (provenance) for adding triple with dct:publisher */
  def loadURI(uriString: String): Action[AnyContent] = {
    recoverFromOutOfMemoryErrorGeneric[Action[AnyContent]](
      {
        Action { implicit request: Request[_] =>
//         val resultFuture = Future {
          val uri = ops.URI(URLDecoder.decode(uriString, "UTF-8"))
          val httpRequest = copyRequest(request)
//          println( s">>>> loadURI: httpRequest $httpRequest")
          val tryGraph = retrieveURIBody(
            uri, dataset, httpRequest, transactionsInside = true)
          val result:String = tryGraph match {
            case Success(gr)           =>
              val rr = wrapInTransaction {
                // add triple <uri> dct:publisher <dct:publisher> to graph <uri> */
                httpRequest.getHTTPparameterValue("publisher")  match {
                  case Some(publisherURI) =>
                  val triple = ops.Triple( uri, dct("publisher"),
                    ops.URI(publisherURI) )
//                  println( s">>>> loadURI: publisher URI <$publisherURI> , $triple")
                  rdfStore.appendToGraph(dataset, uri, ops.makeGraph(
                    Seq( triple ) ) )
                  case None => println( s">>>> loadURI: <$uri> no publisher URI")
                }
              s"Success loading <$uriString>, size: ${ gr.size() }"
              }
              rr . toString()
            case scala.util.Failure(f) => f.getLocalizedMessage
          }
          logger.info("Task ended: " +result)
//         }
//         Ok("Task started: " + resultFuture.toString())
         Ok("Task result: " + result)
        }
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in importing URI"))
  }

  def loadURIpost(): Action[AnyContent] = {
    recoverFromOutOfMemoryErrorGeneric[Action[AnyContent]](
      {
        Action { implicit request: Request[AnyContent] =>
          def loadURI(uri: String): String = {
            val tryGraph = retrieveURIBody(
              ops.URI(uri), dataset, copyRequest(request), transactionsInside = true)
            val result = tryGraph match {
              case Success(gr)           => s"Success loading <$uri>, size: ${gr.size()}"
              case scala.util.Failure(f) => f.getLocalizedMessage
            }
            result
          }

          val content = getContent(request)
          logger.info("loadURIpost " + content)
          val finalResults = content match {
            case None =>
              Ok("ERROR")
            case Some(urisString) =>
              val uris = urisString.split("\n")
              val results = for (uri <- uris) yield {
                loadURI(uri)
              }
              Ok(results.mkString("\n"))
          }
          finalResults
        }
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in POST /load-uri"))
  }

  def wellKnownVoid(): Action[AnyContent] = {
    Action { implicit request: Request[AnyContent] =>
    val file = "void.ttl"
    val void =
    if (new File(file).exists()) {
      scala.io.Source.fromFile( file, "UTF-8").getLines().mkString("\n")
    } else
      "# VOID file void.ttl not provided"

      Ok(void).as("text/turtle; charset=utf-8")
    }
  }

}