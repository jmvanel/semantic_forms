package controllers

import java.net.URLDecoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import deductions.runtime.jena.ApplicationFacadeJena
import deductions.runtime.views.ToolsPage
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Controller
import play.api.mvc.Request
import views.MainXmlWithHead
import deductions.runtime.services.CORS
import deductions.runtime.services.DefaultConfiguration
import play.api.Play
import scala.util.Try
import scala.util.Success
import play.api.mvc.Call
import play.api.http.MimeTypes
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.RDFPrefixes
import play.api.mvc.Result
import play.api.mvc.RequestHeader
import play.api.http.MediaRange


/** main controller */
trait ApplicationTrait extends Controller
    with DefaultConfiguration
    with ApplicationFacadeJena
    with LanguageManagement
    with Secured
    with MainXmlWithHead
    with CORS
    with HTTPrequestHelpers
    with RDFPrefixes[ImplementationSettings.Rdf]
    {

  override def serverPort = {
    val port = Play.current.configuration.
      getString("http.port")
    port match {
      case Some(p) => 
        println("Running on port " + p)
        p
      case _ =>
        println("Retrieving default port from config." )
        super.serverPort
    }
  }

  def index() =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val userInfo = displayUser(userid, "", "", lang)
          Ok("<!DOCTYPE html>\n" + mainPage(<p>...</p>, userInfo, lang))
            .as("text/html; charset=utf-8")
    }

  def displayURI(uri0: String, blanknode: String = "", Edit: String = "",
                 formuri: String = "") =
    withUser {
      implicit userid =>
        implicit request =>
          println(s"""displayURI: $request IP ${request.remoteAddress}, host ${request.host}
            displayURI headers ${request.headers}
            displayURI tags ${request.tags}
            userid <$userid>
            formuri <$formuri>
            displayURI: Edit "$Edit" """)
          val lang = chooseLanguage(request)
          val uri = expandOrUnchanged(uri0)
          println(s"expandOrUnchanged $uri")
          val title = labelForURITransaction(uri, lang)
          outputMainPage(
            htmlForm(uri, blanknode, editable = Edit != "", lang, formuri, graphURI = makeAbsoluteURIForSaving(userid)),
            lang, title = title)
    }

  def form(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "") =
    withUser {
      implicit userid =>
        implicit request =>
          println(s"""form: request $request : "$Edit" formuri <$formuri> """)
          val lang = chooseLanguage(request)
          Ok(htmlForm(uri, blankNode, editable = Edit != "", lang, formuri, graphURI = makeAbsoluteURIForSaving(userid)))
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

  def wordsearchAction(q: String = "") = Action.async {
    implicit request =>
    val lang = chooseLanguageObject(request).language
    val fut: Future[Elem] = wordsearch(q, lang)
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

  def makeAbsoluteURIForSaving(userid: String): String = userid

  /**
   * get RDF with content negotiation (conneg) for RDF syntax;
   *  see also LDP.scala
   *
   *  cf https://www.playframework.com/documentation/2.3.x/ScalaStream
   */
  def downloadAction(url: String) =
    withUser {
      implicit userid =>
        implicit request =>
          def output(accepts: Accepting): Result = {
            val mime = computeMIME(accepts, AcceptsJSONLD)
            Ok.chunked(
              download(url, mime.mimeType)).
              as(s"${mime.mimeType}; charset=utf-8")
              .withHeaders("Access-Control-Allow-Origin" -> "*")
          }
          // Ok.stream(download(url) >>> Enumerator.eof).as("text/turtle; charset=utf-8")
          renderResult(output)
    }


  //// factor out the conneg stuff ////

  val AcceptsTTL = Accepting("text/turtle")
	val AcceptsJSONLD = Accepting("text/json-ld")
	val AcceptsRDFXML = Accepting("application/rdf+xml")
	// format = "turtle" or "rdfxml" or "jsonld"
	val mimeAbbrevs = Map( AcceptsTTL -> "turtle", AcceptsJSONLD -> "rdfxml", AcceptsRDFXML -> "jsonld",
	    Accepts.Json -> "json", Accepts.Xml -> "xml" )

	private def renderResult(output: Accepting => Result, default: Accepting = AcceptsTTL)(implicit request: RequestHeader): Result = {
    render {
      case AcceptsTTL    => output(AcceptsTTL)
      case AcceptsJSONLD => output(AcceptsJSONLD)
      case AcceptsRDFXML => output(AcceptsRDFXML)
      case Accepts.Json  => output(Accepts.Json)
      case Accepts.Xml   => output(Accepts.Xml)
      case _             => output(default)
    }
  }

  val mimeSet = Set(AcceptsTTL, AcceptsJSONLD, AcceptsRDFXML, Accepts.Json, Accepts.Xml)
  private def computeMIME(accepts: Accepting, default: Accepting): Accepting = {
    if( mimeSet.contains(accepts))
       accepts
    else default
  }

  private def computeMIME(accepts: Seq[MediaRange], default: Accepting): Accepting = {
    val v = accepts.find { x => val acc = Accepting(x.mediaType) ; mimeSet.contains(acc) }
    v match {
      case Some(acc) => Accepting(acc.mediaType)
      case None => default
    }
  }

  private def getFirstNonEmptyInMap(map: Map[String, Seq[String]],
                            uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("")
  }

  def sparql(query: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println("sparql: " + request)
          println("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(sparqlConstructQuery(query, lang), lang)
    }

  /**
   * construct or select SPARQL query
   *  conneg => RDF/XML, Turtle or json-ld
   */
  def sparqlConstruct(query: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println(s"""sparqlConstruct: sparql: request $request
            sparql: $query
            accepts ${request.acceptedTypes} """)
          val lang = chooseLanguage(request)

          // TODO better try a parse of the query
          def checkSPARQLqueryType(query: String) =
            if (query.contains("select") ||
              query.contains("SELECT"))
              "select"
            else
              "construct"
          val isSelect = (checkSPARQLqueryType(query) == "select")
          val defaultMIME = if (isSelect) Accepts.Xml else AcceptsJSONLD
          val accepts = request.acceptedTypes
          val mime = computeMIME(accepts, defaultMIME)

          def output(accepts: Accepting): Result = {
            val format = mimeAbbrevs(accepts)
            println(s"sparqlConstruct: accepts: $accepts, format: $format")
            val (result, defaultMIME) = if (isSelect)
              (sparqlSelectConneg(query, format, dataset), Accepts.Xml)
            else
              (sparqlConstructResult(query, lang, format), AcceptsJSONLD)

            val mime = computeMIME(accepts, defaultMIME)
            Ok(result).as(s"${mime.mimeType}; charset=utf-8")
          }

          renderResult(output, mime)
    }

  /** select UI */
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

  val acceptsTurtle = Accepting("text/turtle")
  val turtle = acceptsTurtle.mimeType
  val acceptsJSONLD = Accepting("application/ld+json") // text/json-ld")

  def ldp(uri: String) =
    withUser {
      implicit userid =>
        implicit request =>
          println("LDP GET: request " + request)
          val acceptedTypes = request.acceptedTypes
          println(s"acceptedTypes $acceptedTypes")
          val mimeType =
            if (acceptedTypes.contains(acceptsTurtle))
              turtle
            // tODO RDF/XML
            else
              acceptsJSONLD.mimeType
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

  def lookupService(search: String) = {
    Action { implicit request =>
      println("Lookup: " + request)
      Ok(lookup(search)).as("text/json-ld; charset=utf-8")
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
      Ok(new ToolsPage with DefaultConfiguration {}.getPage(lang) )
        .as("text/html; charset=utf-8")
    }
  }

  def makeHistoryUserActionsAction(userURI: String) =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguage(request)
          outputMainPage(makeHistoryUserActions(userURI, lang, copyRequest(request) ), lang)
    }

}
