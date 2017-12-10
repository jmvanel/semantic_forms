package controllers

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result

import deductions.runtime.services.RDFContentNegociation
import java.net.URLEncoder

/** controller for non-SPARQL Services (or SPARQL related but not in the W3C recommendations) */
trait Services extends ApplicationTrait
with RDFContentNegociation
{

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

      Ok(createDataAsJSON(classUri, chooseLanguage(request),
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
//          println(log("downloadAction", request))
          Ok.chunked(
            // TODO >>>>>>> add database arg.
            download(url, mime)).
            as(s"${mime}; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
        }
        // Ok.stream(download(url) >>> Enumerator.eof).as("text/turtle; charset=utf-8")

        val accepts = httpRequest.getHTTPheaderValue("Accept")
        val mime = computeMIMEOption(accepts) // , defaultMIME)

        val syntaxOption = httpRequest.getHTTPparameterValue("syntax")
//        println((s">>>>>>>> downloadAction syntaxOption $syntaxOption"))
        syntaxOption match {
          case Some(syntax) =>
            val mimeOption = stringMatchesRDFsyntax(syntax)
//            println((s">>>>>>>> downloadAction , mimeOption $mimeOption"))
            mimeOption match {
              case Some(mimeString) =>
                val mime = (mimeString)
//                println((s">>>>>>>>=== downloadAction mimeString $mimeString, mime $mime"))
                output(mime)
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
            editable = (Edit != ""),
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
          editable = (Edit != ""),
          formuri,
          httpRequest))
  }

  /** LDP GET */
  def ldp(uri: String) = Action {
    implicit request: Request[_] =>
      logger.info("LDP GET: request " + request)
      val acceptedTypes = request.acceptedTypes
      logger.info(
          s"acceptedTypes $acceptedTypes")

      val httpRequest = copyRequest(request)
      val accept = httpRequest.getHTTPheaderValue("Accept")
      val firstMimeTypeAccepted = accept.getOrElse("").replaceFirst(",.*", "")
      val mimeType =
        if( isKnownRdfSyntax(firstMimeTypeAccepted) ||
            firstMimeTypeAccepted == htmlMime )
          firstMimeTypeAccepted
        else
          jsonldMime

      println(s">>>> ldp: mimeType $mimeType")
      if (mimeType != htmlMime) {
        val response = getTriples(uri, request.path, mimeType, copyRequest(request))
        logger.info("LDP: GET: result " + response)
        val contentType = mimeType + "; charset=utf-8"
        logger.info(s"contentType $contentType")
        Ok(response)
          .as(contentType)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(CONTENT_TYPE -> mimeType)
      } else {
//    	  println(s">>>> ldp: Redirect /display?displayuri=http://${request.host}/ldp/$uri")
        val ldpURL = "http://" + request.host + "/ldp/" + URLEncoder.encode(uri, "UTF-8")
//    	  println(s">>>> ldp: Redirect $ldpURL")
        val call = Redirect("/display", Map("displayuri" -> Seq(ldpURL)))
//        println(s">>>> ldp: call $call")
        call
      }
  }

  /** TODO:
   * - maybe the stored named graph should be user specific
   * - this is blocking code !!!
   */
  def ldpPOSTAction(uri: String) =
    withUser {
      implicit userid =>
        implicit request =>
          logger.info("LDP: " + request)
          val slug = request.headers.get("Slug")
          val link = request.headers.get("Link")
          val contentType = request.contentType
          val content = getContent(request)
          logger.info(s"LDP: slug: $slug, link $link")
          logger.info(s"LDP: content: $content")
          val serviceCalled =
            ldpPOST(uri, link, contentType, slug, content, copyRequest(request) ).getOrElse("default")
          Ok(serviceCalled).as("text/plain; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
    }




  //  implicit val myCustomCharset = Codec.javaSupported("utf-8") // does not seem to work :(

  def lookupService(search: String, clas: String = "") = {
    Action { implicit request: Request[_] =>
      logger.info(s"""Lookup: $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val mime = request.acceptedTypes.headOption.map { typ => typ.toString() }.getOrElse(Accepts.Xml.mimeType)
      logger.info(s"mime $mime")
      Ok(lookup(search, lang, clas, mime)).as(s"$mime; charset=utf-8")
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }

  def httpOptions(path: String) = {
	  Action { implicit request =>
      logger.info("OPTIONS: " + request)
      Ok("OPTIONS: " + request)
        .as("text/html; charset=utf-8")
        .withHeaders(corsHeaders.toList:_*)
    }
  }
}
