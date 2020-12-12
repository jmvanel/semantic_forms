package controllers

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.http.HeaderNames._
import play.api.mvc.Codec

import scalaz.Scalaz._

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.CreationAbstractForm
import deductions.runtime.utils.RDFContentNegociation
import deductions.runtime.core.MapUtils
import deductions.runtime.services.FormJSON
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.utils.FormModuleBanana

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

/** Services providing raw Forms in JSON format (for external renderers) */
class FormServicesApp @Inject() (
  components: ControllerComponents, configuration: play.api.Configuration) extends {
    override implicit val config = new PlayDefaultConfiguration(configuration)
  }
  with AbstractController(components)
  with RDFStoreLocalJenaProvider
  with FormModuleBanana[ImplementationSettings.Rdf]
with CreationAbstractForm[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with FormJSON[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with RDFContentNegociation
with RDFContentNegociationPlay
with LanguageManagement
with MapUtils {

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


  protected def makeJSONResult(json: String) = makeResultMimeType(json, jsonldMime)
  implicit val myCustomCharset = Codec.javaSupported("utf-8")

  protected def makeResultMimeType(content: String, mimeType: String) =
    Ok(content) .
         as( jsonldMime +
             "; charset=" + myCustomCharset.charset )
         .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
}
