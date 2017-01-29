package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeSeq

import deductions.runtime.jena.ApplicationFacadeJena
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.CORS
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.views.ToolsPage
import play.api.GlobalSettings
import play.api.http.MediaRange
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Codec
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results
import views.MainXmlWithHead

object Global extends GlobalSettings with Results {
  override def onBadRequest(request: RequestHeader, error: String) = {
    Future{ BadRequest("""Bad Request: "$error" """) }
  }
}

/** main controller */
trait ApplicationTrait extends Controller
    with ApplicationFacadeJena
    with LanguageManagement
    with Secured
    with MainXmlWithHead
    with CORS
    with HTTPrequestHelpers
    with RDFPrefixes[ImplementationSettings.Rdf] {

	implicit val myCustomCharset = Codec.javaSupported("utf-8")

	/** a copy of the request with no Play dependency :) */
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

  def index() =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val userInfo = displayUser(userid, "", "", lang)
          val route = routes.Application.makeHistoryUserActionsAction("")
          Redirect(route)
//          Ok("<!DOCTYPE html>\n" + mainPage(<p>...</p>, userInfo, lang))
//            .as("text/html; charset=utf-8")
    }

  def displayURI(uri0: String, blanknode: String = "", Edit: String = "",
                 formuri: String = "") =
    withUser {
      implicit userid =>
        implicit request =>
          //        override def getRequest: HTTPrequest = getRequestCopy()
          println(s"""displayURI: $request IP ${request.remoteAddress}, host ${request.host}
            displayURI headers ${request.headers}
            displayURI tags ${request.tags}
            userid <$userid>
            formuri <$formuri>
            displayURI: Edit "$Edit" """)
          val lang = chooseLanguage(request)
          val uri = expandOrUnchanged(uri0)
          println(s"displayURI: expandOrUnchanged $uri")
          val title = labelForURITransaction(uri, lang)
          outputMainPage(
            htmlForm(uri, blanknode, editable = Edit != "", lang, formuri,
              graphURI = makeAbsoluteURIForSaving(userid),
              request = getRequestCopy()),
            lang, title = title)
    }

  def form(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "", database: String = "TDB") =
//        Action // 
        withUser
    {
      implicit userid =>
        implicit request =>
          println(s"""form: request $request : "$Edit" formuri <$formuri> """)
          val lang = chooseLanguage(request)
          Ok(htmlForm(uri, blankNode, editable = Edit != "", lang, formuri,
              graphURI = makeAbsoluteURIForSaving(userid), database=database))
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
    }

  /** /form-data service; like /form but raw JSON data */
  def formData(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "", database: String = "TDB") =
//    withUser
    Action
    {
//      implicit userid =>
        implicit request =>
       makeJSONResult(formDataImpl(uri, blankNode, Edit, formuri, database))
    }

  private def makeJSONResult(json: String) = makeResultMimeType(json, AcceptsJSONLD.mimeType)
  
  private def makeResultMimeType(content: String, mimeType: String) =
    Ok(content) .
         as( AcceptsJSONLD.mimeType + "; charset=" + myCustomCharset.charset )
         .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
         .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")

  /**
   * /sparql-form service: Create HTML form from SPARQL (construct);
   *  like /sparql has input a SPARQL query;
   *  like /form and /display has input Edit, formuri & database
   */
  def sparqlForm(query: String, Edit: String = "", formuri: String = "", database: String = "TDB") = Action {
    makeJSONResult(
      createHTMLFormFromSPARQL(
        query,
        editable = Edit != "",
        formuri).toString() )
    .as("text/html; charset=utf-8")
  }

  def searchOrDisplayAction(q: String) = {
//          withUser {
//	    implicit userid =>
//      implicit request => {
    def isURI(q: String): Boolean = q.contains(":")
    
    if (isURI(q))
      displayURI( q, Edit="" )
    else
      wordsearchAction(q)
  }
    
  /** generate a Main Page wrapping given XHTML content */
  private def outputMainPage( content: NodeSeq,
      lang: String, userInfo: NodeSeq = <div/>, title: String = "" )
  (implicit request: Request[_]) = {
      Ok( "<!DOCTYPE html>\n" +
        mainPage( content, userInfo, lang, title )
      ).withHeaders("Access-Control-Allow-Origin" -> "*") // for dbpedia lookup
      .as("text/html; charset=utf-8")
  }

  def wordsearchAction(q: String = "", clas: String = "") = Action.async {
    implicit request =>
    val lang = chooseLanguageObject(request).language
    val fut: Future[Elem] = wordsearch(q, lang, clas)
    fut.map( r => outputMainPage( r, lang ) )
  }

  /** pasted from above */
  def showNamedGraphsAction() = Action.async {
    implicit request =>
    val lang = chooseLanguageObject(request).language
    val fut = showNamedGraphs(lang)
    val rr = fut.map( r => outputMainPage( r, lang ) )
    rr
  }

  def showTriplesInGraphAction( uri: String) = {
        Action.async { implicit request =>
          val lang = chooseLanguageObject(request).language
          val fut = Future.successful( showTriplesInGraph( uri, lang) )
          val rr = fut.map( r => outputMainPage( r, lang ) )
          rr
  }
  }

  /////////////////////////////////

  def edit(uri: String) =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val pageURI = uri
          val pageLabel = labelForURI(uri, lang)
          val userInfo = displayUser(userid, pageURI, pageLabel, lang)
          println(s"userInfo $userInfo, userid $userid")
          val content = htmlForm(
            uri, editable = true,
            lang = chooseLanguage(request), graphURI = makeAbsoluteURIForSaving(userid))
          Ok("<!DOCTYPE html>\n" + mainPage(content, userInfo, lang))
            .as("text/html; charset=utf-8").
            withHeaders("Access-Control-Allow-Origin" -> "*") // TODO dbpedia only
    }

  /** */
  def saveAction() =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguage(request)
//          outputMainPage(save(request, userid, graphURI=makeAbsoluteURIForSaving(userid)), lang)
          val uri = saveOnly(request, userid, graphURI=makeAbsoluteURIForSaving(userid))
          println(s"saveAction: uri $uri")
          val call = routes.Application.displayURI(uri)
          Redirect(call)
          /* TODO */
          // recordForHistory( userid, request.remoteAddress, request.host )
    }

  private def saveOnly(request: Request[_], userid: String, graphURI: String = ""): String = {
    val body = request.body
    val host  = request.host
    body match {
      case form: AnyContentAsFormUrlEncoded =>
        val lang = chooseLanguage(request)
        val map = form.data
        println(s"ApplicationTrait.save: ${body.getClass}, map $map")
        // cf http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
        val subjectUriTryOption = Try {
          saveForm(map, lang, userid, graphURI, host)
        }
        subjectUriTryOption match {
            case Success(Some(url1)) => url1
            case _ => ""
        }
    }
  }
  
//  /** UNUSED */
//  private def save(request: Request[_], userid: String, graphURI: String = "" ): NodeSeq = {
//      val body = request.body
//      body match {
//        case form: AnyContentAsFormUrlEncoded =>
//          val lang = chooseLanguage(request)
//          val map = form.data
//          println(s"ApplicationTrait.save: ${body.getClass}, map $map")
//          // cf http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
//          val subjectUriTryOption = Try {
//            saveForm( map, lang, userid, graphURI )
//          }
//          println(s"ApplicationTrait.save: uriOption $subjectUriTryOption, userid $userid")
//          subjectUriTryOption match {
//            case Success(Some(url1)) =>
//              htmlForm( URLDecoder.decode(url1, "utf-8"),
//              editable = false,
//              lang = lang )
//            case _ => <p>Save: not normal: { subjectUriTryOption }</p>
//          }
//        case _ => <p>Save: not normal: { getClass() }</p>
//      }
//  }

  /** creation form - generic SF application */
  def createAction() =
    withUser {
      implicit userid =>
        implicit request =>
          println("create: " + request)
          // URI of RDF class from which to create instance
          val uri0 = getFirstNonEmptyInMap(request.queryString, "uri")
          val uri = expandOrUnchanged(uri0)
          // URI of form Specification
          val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
          println("create: " + uri)
          println( s"formSpecURI from HTTP request: <$formSpecURI>")
          val lang = chooseLanguage(request)
          outputMainPage(
            create(uri, chooseLanguage(request),
              formSpecURI, makeAbsoluteURIForSaving(userid), copyRequest(request) ),
            lang)
    }

  private def makeAbsoluteURIForSaving(userid: String): String = userid

  /** creation form as raw JSON data
   *  TODO add database HTTP param. */
  def createData() =
    withUser {
      implicit userid =>
        implicit request =>
          println("create: " + request)
          // URI of RDF class from which to create instance
          val uri0 = getFirstNonEmptyInMap(request.queryString, "uri")
          val uri = expandOrUnchanged(uri0)
          // URI of form Specification
          val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
          println("create: " + uri)
          println( s"formSpecURI from HTTP request: <$formSpecURI>")

          Ok( createDataAsJSON( uri, chooseLanguage(request),
                       formSpecURI, makeAbsoluteURIForSaving(userid), copyRequest(request) ) ) .
                       as( AcceptsJSONLD.mimeType + "; charset=" + myCustomCharset.charset )
    }

  /**
   * get RDF with content negotiation (conneg) for RDF syntax;
   *  see also LDP.scala
   *
   *  cf https://www.playframework.com/documentation/2.3.x/ScalaStream
   */
  def downloadAction(url: String, database: String = "TDB") =
    withUser {
      implicit userid =>
        implicit request =>
          def output(accepts: Accepting): Result = {
            val mime = computeMIME(accepts, AcceptsJSONLD)
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


  //// factor out the conneg stuff ////

  private val AcceptsTTL = Accepting("text/turtle")
	private val AcceptsJSONLD = Accepting("application/ld+json")
	private val AcceptsRDFXML = Accepting("application/rdf+xml")
	private val AcceptsSPARQLresults = Accepting("application/sparql-results+json")

	private val turtle = AcceptsTTL.mimeType

	// format = "turtle" or "rdfxml" or "jsonld"
	val mimeAbbrevs = Map( AcceptsTTL -> "turtle", AcceptsJSONLD -> "jsonld", AcceptsRDFXML -> "rdfxml",
	    Accepts.Json -> "json", Accepts.Xml -> "xml", AcceptsSPARQLresults -> "json" )

	private def renderResult(output: Accepting => Result, default: Accepting = AcceptsTTL)(implicit request: RequestHeader): Result = {
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
  private def computeMIME(accepts: Accepting, default: Accepting): Accepting = {
    if( mimeSet.contains(accepts))
       accepts
    else default
  }

  private def computeMIME(accepts: Seq[MediaRange], default: Accepting): Accepting = {
    val v = accepts.find {
      mediaRange => val acc = Accepting(mediaRange.toString())
      mimeSet.contains(acc) }
    v match {
      case Some(acc) => Accepting(acc.toString())
      case None => default
    }
  }

  private def getFirstNonEmptyInMap(map: Map[String, Seq[String]],
                            uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("")
  }

  /** SPARQL Construct UI */
  def sparql(query: String) =
//    withUser
    Action {
//      implicit userid =>
        implicit request =>
          println("sparql: " + request)
          println("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(
//        		  { sparqlQueryForm(query, "/sparql-ui",
//        				  Seq("CONSTRUCT {?S ?P ?O . } WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10" )) } ++
              sparqlConstructQuery(query, lang), lang)
    }

  /**
   * SPARQL GET compliant, construct or select
   * conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlConstruct(query: String) =
        Action {
//    withUser {
//      implicit userid =>
        implicit request =>
          println(s"""sparqlConstruct: sparql: request $request
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

//          val defaultMIME = if (isSelect) Accepts.Xml else AcceptsJSONLD
//          val accepts = request.acceptedTypes
//          val mime = AcceptsSPARQLresults

//          def output(accepts: Accepting): Result = {
//            val format = mimeAbbrevs(accepts)
//            println(s"sparqlConstruct: output(accepts=$accepts) => format: $format")
//            val (result, defaultMIME) = if (isSelect)
//              (sparqlSelectConneg(query, format, dataset), AcceptsSPARQLresults) // Accepts.Xml)
//            else
//              (sparqlConstructResult(query, lang, format), AcceptsJSONLD)
//
//            val mime = computeMIME(accepts, defaultMIME)
//            println( s"sparqlConstruct (output): mime.mimeType ${mime.mimeType}")
//            Ok(result).as(s"${mime.mimeType}; charset=utf-8")
//          }
//          println( s"sparqlConstruct: mime ${mime}")
          
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
    implicit request =>
      println(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val body: AnyContent = request.body

      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded
      val r = formBody.map { map =>

        val query0 = map.getOrElse("query", Seq())
        val query = query0 . mkString("\n")
        println(s"""sparql: $query""" )

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

  /** output SPARQL query as Play! Result;
   *  priority to accepted MIME type */
  private def outputSPARQL(query: String, acceptedTypes: Seq[MediaRange], isSelect: Boolean): Result = {
    val preferredMedia = acceptedTypes.map { media => Accepting(media.toString()) }.headOption
    val defaultMIMEaPriori = if (isSelect) AcceptsSPARQLresults else AcceptsJSONLD
    val defaultMIME = preferredMedia.getOrElse(defaultMIMEaPriori)
    val mime = computeMIME(acceptedTypes, defaultMIME)
    println(s"sparqlConstruct: computed mime ${mime}")

    val resultFormat = mimeAbbrevs(defaultMIME) // preferredMedia.getOrElse(defaultMIME))
    println(s"sparqlConstruct: output(accepts=$acceptedTypes) => result format: $resultFormat")
    if (preferredMedia.isDefined &&
      !mimeSet.contains(preferredMedia.get))
      println(s"CAUTION: preferredMedia $preferredMedia not in this application's list: ${mimeAbbrevs.keys.mkString(", ")}")
    val result = if (isSelect)
      sparqlSelectConneg(query, resultFormat, dataset)
    else
      sparqlConstructResult(query, resultFormat)

    println(s"outputSPARQL: mime.mimeType ${mime.mimeType}")
    println(s"result $result".split("\n").take(5).mkString("\n"))
    Ok(result)
      .as(s"${mime.mimeType}")
    // .as(s"${mime.mimeType}; charset=utf-8")
  }
          
  /**
   * service /sparql-data, like /form-data spits raw JSON data for a view,
   * but from a SPARQL CONSTRUCT query,
   *  cf issue https://github.com/jmvanel/semantic_forms/issues/115
   */
  def sparqlDataPOST = Action {
    // TODO pasted from sparqlConstructPOST
    implicit request =>
      println(s"""sparqlConstruct: sparql: request $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val body: AnyContent = request.body

      // Expecting body as FormUrlEncoded
      val formBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded
      val result = formBody.map { map =>

        val query0 = map.getOrElse("query", Seq())
        val query = query0.mkString("\n")
        println(s"""sparql: $query""")

        val Edit = map.getOrElse("Edit", Seq()).headOption.getOrElse("")
        val formuri = map.getOrElse("formuri", Seq()).headOption.getOrElse("")

        makeJSONResult(
          createJSONFormFromSPARQL(
            query,
            editable = Edit != "",
            formuri))
      }

      result match {
        case Some(r) => r
        case None    => BadRequest("sparqlDataPOST: BadRequest: nothing in form Body")
      }
  }
  
  /** SPARQL select UI */
  def select(query: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println("sparql: " + request)
          println("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(
            sparqlSelectQuery(query, lang), lang)
    }

  def update(update: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println("sparql update: " + request)
          println("sparql: " + update)
          val res = sparqlUpdateQuery(update)
          res match {
            case Success(s) => Ok(s"$res")
            case Failure(f) => InternalServerError(s"$res")
          }
    }

  def backlinksAction(q: String = "") = Action.async {
	  implicit request =>
	  val fut: Future[Elem] = backlinks(q)
    val extendedSearchLink = <p>
                               <a href={ "/esearch?q=" + q }>
                                 Extended Search for &lt;{ q }
                                 &gt;
                               </a>
                             </p>
    fut.map { res =>
    val lang = chooseLanguage(request)
    outputMainPage(
        NodeSeq fromSeq Seq(extendedSearchLink, res), lang)
    }
  }

  def extSearch(q: String = "") = Action.async {
	  implicit request =>
	  val lang = chooseLanguage(request)
    val fut = esearch(q)
    fut.map(r =>
    outputMainPage(r, lang))
  }

  def ldp(uri: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println("LDP GET: request " + request)
          val acceptedTypes = request.acceptedTypes
          println(s"acceptedTypes $acceptedTypes")
          val mimeType =
            if (acceptedTypes.contains(AcceptsTTL))
              turtle
            // TODO RDF/XML
            else
              AcceptsJSONLD.mimeType
          val response = ldpGET(uri, request.path, mimeType, copyRequest(request))
          println("LDP: GET: result " + response)
          val contentType = mimeType + "; charset=utf-8"
          println(s"contentType $contentType")
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
          println("LDP: " + request)
          val slug = request.headers.get("Slug")
          val link = request.headers.get("Link")
          val contentType = request.contentType
          val content = {
            val asText = request.body.asText
            if (asText != None) asText
            else {
              val raw = request.body.asRaw.get
              println(s"""LDP: raw: "$raw" size ${raw.size}""")
              raw.asBytes(raw.size.toInt).map {
                arr => new String(arr.toArray, "UTF-8")
              }
            }
          }
          println(s"LDP: slug: $slug, link $link")
          println(s"LDP: content: $content")
          val serviceCalled =
            ldpPOST(uri, link, contentType, slug, content, copyRequest(request) ).getOrElse("default")
          Ok(serviceCalled).as("text/plain; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
    }

//  implicit val myCustomCharset = Codec.javaSupported("utf-8") // does not seem to work :(

  def lookupService(search: String, clas: String = "") = {
    Action { implicit request =>
      println(s"""Lookup: $request
            accepts ${request.acceptedTypes} """)
      val lang = chooseLanguage(request)
      val mime = request.acceptedTypes.headOption.map { typ => typ.toString() }.getOrElse(Accepts.Xml.mimeType)
      println(s"mime $mime")
      Ok(lookup(search, lang, clas, mime)).as(s"$mime; charset=utf-8")
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }

  def httpOptions(path: String) = {
	  Action { implicit request =>
      println("OPTIONS: " + request)
      Ok("OPTIONS: " + request)
        .as("text/html; charset=utf-8")
        .withHeaders(corsHeaders.toList:_*)
    }
  }

  def toolsPage = {
    Action { implicit request =>
      val lang = chooseLanguage(request)
      Ok(new ToolsPage with DefaultConfiguration {
        override def getRequest: HTTPrequest = getRequestCopy()
      }.getPage(lang) )
        .as("text/html; charset=utf-8")
    }
  }

  def makeHistoryUserActionsAction(limit: String) =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguage(request)
          println("makeHistoryUserActionsAction: cookies: " + request.cookies.mkString("; "))
          outputMainPage(makeHistoryUserActions(limit, lang, copyRequest(request) ), lang)
    }

}
