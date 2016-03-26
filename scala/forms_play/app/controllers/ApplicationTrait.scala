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


/** main controller */
trait ApplicationTrait extends Controller
    with DefaultConfiguration
    with ApplicationFacadeJena
    with LanguageManagement
    with Secured
    with MainXmlWithHead
    with CORS
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
    implicit request => {
      val lang = chooseLanguageObject(request).language
      val userInfo = displayUser(userid, "", "", lang)
      Ok( "<!DOCTYPE html>\n" + mainPage(<p>...</p>, userInfo, lang))
            .as("text/html; charset=utf-8")
    }
  }

  def displayURI(uri: String, blanknode: String = "", Edit: String = "",
      formuri: String="") =
      //    { Action { implicit request =>
      withUser {
	    implicit userid =>
      implicit request => {
      println(s"""displayURI: $request IP ${request.remoteAddress}, host ${request.host}
         displayURI headers ${request.headers}
         displayURI tags ${request.tags}
         userid <$userid>
         formuri <$formuri>
         displayURI: Edit "$Edit" """)
      val lang = chooseLanguage(request)
      val title = labelForURITransaction(uri, lang)
      outputMainPage(
        htmlForm(uri, blanknode, editable = Edit != "", lang, formuri, graphURI=makeAbsoluteURIForSaving(userid)),
        lang, title=title )
      // TODO record in TDB for history: userid, request.remoteAddress, request.host
    }
  }

  def form(uri: String, blankNode: String = "", Edit: String = "", formuri: String ="") =
//    { Action { implicit request =>
          withUser {
	    implicit userid =>
      implicit request => {
      println( s"""form: request $request : "$Edit" formuri <$formuri> """)
      val lang = chooseLanguage(request)
      Ok(htmlFormElemJustFields(uri: String, hrefDisplayPrefix, blankNode,
        editable = Edit != "", lang, formuri, graphURI=makeAbsoluteURIForSaving(userid)))
        .as("text/html; charset=utf-8")
    }
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
      println( s"userInfo $userInfo, userid $userid" )
       val content = htmlForm(
        uri, editable = true,
        lang = chooseLanguage(request), graphURI=makeAbsoluteURIForSaving(userid))
      Ok( "<!DOCTYPE html>\n" + mainPage( content, userInfo, lang))
            .as("text/html; charset=utf-8").
        withHeaders("Access-Control-Allow-Origin" -> "*") // TODO dbpedia only
  }

  /** TODO: keep session when Redirect to display page */
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
    }

  private def saveOnly(request: Request[_], userid: String, graphURI: String = ""): String = {
    val body = request.body
    body match {
      case form: AnyContentAsFormUrlEncoded =>
        val lang = chooseLanguage(request)
        val map = form.data
        println(s"ApplicationTrait.save: ${body.getClass}, map $map")
        // cf http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
        val subjectUriTryOption = Try {
          saveForm(map, lang, userid, graphURI)
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
          val uri = getFirstNonEmptyInMap(request.queryString, "uri")
          // URI of form Specification
          val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
          println("create: " + uri)
          println("formSpecURI: " + formSpecURI)
          val lang = chooseLanguage(request)
          outputMainPage(
            create(uri, chooseLanguage(request),
              formSpecURI, makeAbsoluteURIForSaving(userid)
              ),
            lang)
    }

  def makeAbsoluteURIForSaving(userid: String): String = userid 
    
  //  def download(url: String): Action[_] = {
  //    Action { Ok(downloadAsString(url)).as("text/turtle; charset=utf-8") }
  //  }

  /** cf https://www.playframework.com/documentation/2.3.x/ScalaStream */
  def downloadAction(url: String) =
    //  {   Action {
    withUser {
      implicit userid =>
        implicit request => {
          Ok.chunked(download(url)).as("text/turtle; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
          // Ok.stream(download(url) >>> Enumerator.eof).as("text/turtle; charset=utf-8")
        }
    }

  def getFirstNonEmptyInMap(map: Map[String, Seq[String]],
                            uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("")
  }

  def sparql(query: String) =
    //  {  Action { implicit request =>
    withUser {
      implicit userid =>
        implicit request => {
          println("sparql: " + request)
          println("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(sparqlConstructQuery(query, lang), lang)
        }
    }

  def select(query: String) =
    //  {  Action { implicit request =>
    withUser {
      implicit userid =>
        implicit request => {
          println("sparql: " + request)
          println("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(
            sparqlSelectQuery(query, lang), lang)
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
    //  {  Action { implicit request =>
    withUser {
      implicit userid =>
        implicit request => {
          println("LDP GET: request " + request)
          val acceptedTypes = request.acceptedTypes // contentType
          val acceptsTurtle = Accepting("text/turtle")
          val turtle = acceptsTurtle.mimeType
          val accepts = Accepting(acceptedTypes.headOption.getOrElse(turtle).toString())
          val r = ldpGET(uri, request.path, accepts.mimeType)
          println("LDP: GET: result " + r)
          val contentType = accepts.mimeType + "; charset=utf-8"
          println(s"contentType $contentType")
          Ok(r).as(contentType)
            .withHeaders("Access-Control-Allow-Origin" -> "*")
        }
    }

  /** TODO:
   * - maybe the stored named graph should be user specific
   * - this is blocking code !!!
   */
  def ldpPOSTAction(uri: String) =
    //  { Action { implicit request =>
    withUser {
      implicit userid =>
        implicit request => {
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
                arr => new String(arr, "UTF-8")
              }
            }
          }
          println(s"LDP: slug: $slug, link $link")
          println(s"LDP: content: $content")
          val serviceCalled =
            ldpPOST(uri, link, contentType, slug, content).getOrElse("default")
          Ok(serviceCalled).as("text/plain; charset=utf-8")
            .withHeaders("Access-Control-Allow-Origin" -> "*")
        }
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
      Ok(new ToolsPage with DefaultConfiguration {}.getPage)
        .as("text/html; charset=utf-8")
    }
  }

  def makeHistoryUserActionsAction(userURI: String) =
    //    Action { implicit request =>
    withUser {
      implicit userid =>
        implicit request => {
          val lang = chooseLanguage(request)
          outputMainPage(makeHistoryUserActions(userURI, lang), lang)
        }
    }

}
