package controllers

import play.api.mvc._
//import java.nio.file.Files

//object Global extends GlobalSettings with Results {
//  override def onBadRequest(request: RequestHeader, error: String) = {
//    Future{ BadRequest("""Bad Request: "$error" """) }
//  }
//}

/** main controller 
 *  TODO split HTML pages & HTTP services */
trait Services extends ApplicationTrait
{

  /** /form-data service; like /form but raw JSON data */
  def formDataAction(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "", database: String = "TDB") =
    Action {
        implicit request: Request[_] =>
        val lang = chooseLanguage(request)
       makeJSONResult(formData(uri, blankNode, Edit, formuri, database, lang))
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
          def output(accepts: Accepting): Result = {
            val mime = computeMIME(accepts, AcceptsJSONLD)
            println(log("downloadAction", request))
            Ok.chunked(
                // TODO >>>>>>> add database arg.
              download(url, mime.mimeType)).
              as(s"${mime.mimeType}; charset=utf-8")
              .withHeaders("Access-Control-Allow-Origin" -> "*")
          }
          // Ok.stream(download(url) >>> Enumerator.eof).as("text/turtle; charset=utf-8")

          val defaultMIME = AcceptsJSONLD
          val accepts = request.acceptedTypes
          val mime = computeMIME(accepts, defaultMIME)

          // TODO generalize outputSPARQL() : give priority to requested MIME type
          renderResult(output, mime)
    }


  /**
   * SPARQL GET compliant, construct or select
   * conneg => RDF/XML, Turtle or json-ld
   * 
   * TODO rename sparqlService
   */
  def sparqlConstruct(query: String) =
        Action {
//    withUser {
//      implicit userid =>
        implicit request: Request[_] =>
          logger.info(s"""sparqlConstruct: sparql: request $request
            sparql: $query
            accepts ${request.acceptedTypes} """)
          val lang = chooseLanguage(request)

          // TODO better try a parse of the query
          def checkSPARQLqueryType(query: String) =
            if (query.toLowerCase().contains("select") )
              "select"
            else
              "construct"

          val isSelect = (checkSPARQLqueryType(query) == "select")
          
          outputSPARQL(query, request.acceptedTypes, isSelect)
//          renderResult(output, default = mime)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
          /* access-control-allow-headers :"Accept, Authorization, Slug, Link, Origin, Content-type, 
           * DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,
           * If-Modified-Since,Cache-Control,Content-Type,Accept-Encoding"
           */
  }

  /**
   * SPARQL POST compliant, construct or select SPARQL query
   *  conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlConstructPOST = Action {
    implicit request: Request[AnyContent] =>
      logger.info(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val body: AnyContent = request.body

      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded
      val r = formBody.map { map =>

        val query0 = map.getOrElse("query", Seq())
        val query = query0 . mkString("\n")
        logger.info(s"""sparql: $query""" )

        // TODO better try a parse of the query
        def checkSPARQLqueryType(query: String) =
          if (query.toLowerCase().contains("select") )
            "select"
          else
            "construct"
        val isSelect = (checkSPARQLqueryType(query) == "select")
        val acceptedTypes = request.acceptedTypes

        outputSPARQL(query, acceptedTypes, isSelect)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      }
      r match {
        case Some(r) => r
        case None => BadRequest("BadRequest")
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
        logger.info(s"""sparql: $query""")

        val Edit = map.getOrElse("Edit", Seq()).headOption.getOrElse("")
        val formuri = map.getOrElse("formuri", Seq()).headOption.getOrElse("")

        makeJSONResult(
          createJSONFormFromSPARQL(
            query,
            editable = (Edit != ""),
            formuri))
      }

      result match {
        case Some(r) => r
        case None    => BadRequest("sparqlDataPOST: BadRequest: nothing in form Body")
      }
  }
  /** LDP GET */
  def ldp(uri: String) =
    Action // withUser 
    {
//      implicit userid =>
        implicit request: Request[_] =>
          logger.info("LDP GET: request " + request)
          val acceptedTypes = request.acceptedTypes
          logger.info(s"acceptedTypes $acceptedTypes")
          val mimeType =
            if (acceptedTypes.contains(AcceptsTTL))
              turtle
            // TODO RDF/XML
            else
              AcceptsJSONLD.mimeType
          val response = getTriples(uri, request.path, mimeType, copyRequest(request))
          logger.info("LDP: GET: result " + response)
          val contentType = mimeType + "; charset=utf-8"
          logger.info(s"contentType $contentType")
          Ok(response)
            //          .as(contentType)
            //          .as(MimeTypes.JSON)
            .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
            .withHeaders(CONTENT_TYPE -> mimeType)
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
