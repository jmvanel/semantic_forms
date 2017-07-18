package controllers

import java.net.URLEncoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

import deductions.runtime.html.TableView
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.html.Form2HTMLBanana
import deductions.runtime.services.html.HTML5TypesTrait
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.views.ToolsPage
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Request


/** controller for HTML pages ("generic application") */
trait WebPages extends Controller with ApplicationTrait {
  import config._

  def index() =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val userInfo = displayUser(userid, "", "", lang)
          outputMainPage(makeHistoryUserActions("10", lang, copyRequest(request) ), lang,
              userInfo = userInfo)
    }

  /** @param Edit edit mode <==> param not "" */
  def displayURI(uri0: String, blanknode: String = "", Edit: String = "",
                 formuri: String = "") =
    if (needLoginForDisplaying || ( needLoginForEditing && Edit !="" ))
      withUser {
        implicit userid =>
          implicit request =>
            //          logger.info(
            println(
              s"""displayURI: $request IP ${request.remoteAddress}, host ${request.host}
            displayURI headers ${request.headers}
            displayURI tags ${request.tags}
            displayURI cookies ${request.cookies.toList}
            userid <$userid>
            formuri <$formuri>
            displayURI: Edit "$Edit" """)
            val lang = chooseLanguage(request)
            val uri = expandOrUnchanged(uri0)
            logger.info(s"displayURI: expandOrUnchanged $uri")
            val title = labelForURITransaction(uri, lang)
            val userInfo = displayUser(userid, uri, title, lang)
            outputMainPage(
              htmlForm(uri, blanknode, editable = Edit != "", lang, formuri,
                graphURI = makeAbsoluteURIForSaving(userid),
                request = getRequestCopy()) . _1 ,
              lang, title = title, userInfo = userInfo)
      }
    else
      Action { implicit request: Request[_] =>
        {
          val lang = chooseLanguage(request)
          val uri = expandOrUnchanged(uri0)
          logger.info(s"displayURI: expandOrUnchanged $uri")
          val title = labelForURITransaction(uri, lang)
          val requestCopy = getRequestCopy()
          val userid = requestCopy . userId()
          val userInfo = displayUser(userid, uri, title, lang)

          outputMainPage(
            htmlForm(uri, blanknode, editable = Edit != "", lang, formuri,
              graphURI = makeAbsoluteURIForSaving(userid),
              request = requestCopy) . _1,
            lang, title = title, userInfo = userInfo)
        }
      }


  
  def table = Action { implicit request: Request[_] =>
    val requestCopy = getRequestCopy()
    val userid = requestCopy.userId()
    val title = "Table view from SPARQL"
    val lang = chooseLanguage(request)
    val userInfo = displayUser(userid, "", title, lang)
    val query = queryFromRequest(requestCopy)
    outputMainPage(
      <div>
        <a href={ "/sparql-ui?query=" + URLEncoder.encode(query) }>Back to SPARQL page</a>
      </div>
        ++
        tableFromSPARQL(requestCopy),
      lang, title = title, userInfo = userInfo,
      classForContent = "")
  }

  private def tableFromSPARQL(request: HTTPrequest): NodeSeq = {
    val query = queryFromRequest(request)
    val formSyntax = createFormFromSPARQL(query,
      editable = false,
      formuri = "", request)
    val tv = new TableView[ImplementationSettings.Rdf#Node, ImplementationSettings.Rdf#URI]
        with Form2HTMLBanana[ImplementationSettings.Rdf]
        with ImplementationSettings.RDFModule
        with HTML5TypesTrait[ImplementationSettings.Rdf]
        with RDFPrefixes[ImplementationSettings.Rdf]{
      val config = new DefaultConfiguration {}
      val nullURI = ops.URI("")
    }
    tv.generate(formSyntax)
  }

  private def queryFromRequest(request: HTTPrequest) =
    request.queryString.getOrElse("query", Seq()).headOption.getOrElse("")




  /** "naked" HTML form */
  def form(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "",
      database: String = "TDB") =
		  Action {
        implicit request: Request[_] =>
          logger.info(s"""form: request $request : "$Edit" formuri <$formuri> """)
          val lang = chooseLanguage(request)
          val requestCopy = getRequestCopy()
          val userid = requestCopy . userId()
          Ok(htmlForm(uri, blankNode, editable = Edit != "", lang, formuri,
              graphURI = makeAbsoluteURIForSaving(userid), database=database) . _1 )
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
          .as("text/html; charset=utf-8")
    }




  /**
   * /sparql-form service: Create HTML form or view from SPARQL (construct);
   *  like /sparql has input a SPARQL query;
   *  like /form and /display has parameters Edit, formuri & database
   */
  def sparqlForm(query: String, Edit: String = "", formuri: String = "", database: String = "TDB") =
    Action { implicit request: Request[_] =>
    val requestCopy = getRequestCopy()
    val userid = requestCopy . userId()
    val lang = chooseLanguage(request)
    val userInfo = displayUser(userid, "", "", lang)
      outputMainPage(
        createHTMLFormFromSPARQL(
          query,
          editable = Edit != "",
          formuri, requestCopy),
        lang, userInfo)
  }

  /** SPARQL Construct UI */
  def sparql(query: String) = {
    logger.info("sparql: " + query)
    def doAction(implicit request: Request[_]) = {
      logger.info("sparql: " + request)
      val lang = chooseLanguage(request)
      outputMainPage(sparqlConstructQuery(query, lang), lang)

        // TODO factorize
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
    }
    if (needLoginForDisplaying)
      Action { implicit request: Request[_] => doAction }
    else
      withUser { implicit userid => implicit request => doAction }
  }

  /** SPARQL select UI */
  def select(query: String) =
    withUser {
      implicit userid =>
        implicit request =>
          logger.info("sparql: " + request)
          logger.info("sparql: " + query)
          val lang = chooseLanguage(request)
          outputMainPage(
            selectSPARQL(query, lang), lang)
    }



  def searchOrDisplayAction(q: String) = {
    def isURI(q: String): Boolean = q.contains(":")
    
    if (isURI(q))
      displayURI( q, Edit="" )
    else
      wordsearchAction(q)
  }

  def wordsearchAction(q: String = "", clas: String = "") = Action.async {
    implicit request: Request[_] =>
    val lang = chooseLanguageObject(request).language
    val fut: Future[Elem] = wordsearchFuture(q, lang, clas)
    fut.map( r => outputMainPage( r, lang ) )
  }

  /** show Named Graphs - pasted from above wordsearchAction */
  def showNamedGraphsAction() = Action.async {
    implicit request: Request[_] =>
    val lang = chooseLanguageObject(request).language
    val fut = showNamedGraphs(lang)
    val rr = fut.map( r => outputMainPage( r, lang ) )
    rr
  }

  /** show Triples In given Graph */
  def showTriplesInGraphAction( uri: String) = {
        Action.async { implicit request: Request[_] =>
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
          val pageLabel = labelForURITransaction(uri, lang)
          val userInfo = displayUser(userid, pageURI, pageLabel, lang)
          logger.info(s"userInfo $userInfo, userid $userid")
          val content = htmlForm(
            uri, editable = true,
            lang = chooseLanguage(request), graphURI = makeAbsoluteURIForSaving(userid),
            request=copyRequest(request) ). _1
          Ok("<!DOCTYPE html>\n" + mainPage(content, userInfo, lang))
            .as("text/html; charset=utf-8").
            withHeaders("Access-Control-Allow-Origin" -> "*") // TODO dbpedia only
    }

  /** save the HTML form;
   *  intranet mode (needLoginForEditing == false): no cookies session, just receive a `graph` HTTP param.
   *  TODO: this pattern should be followed for each page or service */
  def saveAction() = {
    def save(userid: String)(implicit request: Request[_]) = {
      val lang = chooseLanguage(request)
      val (uri, typeChanges) = saveOnly(request, userid, graphURI = makeAbsoluteURIForSaving(userid))
      logger.info(s"saveAction: uri <$uri>")
      val call = routes.Application.displayURI(uri,
          Edit=if(typeChanges) "edit" else "" )
      Redirect(call)
      /* TODO */
      // recordForHistory( userid, request.remoteAddress, request.host )
    }
    if (needLoginForEditing)
      withUser {
        implicit userid =>
          implicit request =>
            save(userid)
      }
    else
      Action { implicit request: Request[_] => {
        val user = request.headers.toMap.getOrElse("graph", Seq("anonymous") ). headOption.getOrElse("anonymous")
        save(user)
      }}
  }

  /** creation form - generic SF application */
  def createAction() =
    withUser {
      implicit userid =>
        implicit request =>
          logger.info(s"create: request $request")
          // URI of RDF class from which to create instance
          val uri0 = getFirstNonEmptyInMap(request.queryString, "uri")
          val uri = expandOrUnchanged(uri0)
          // URI of form Specification
          val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
          logger.info(s"""create: "$uri" """)
          logger.info( s"formSpecURI from HTTP request: <$formSpecURI>")
          val lang = chooseLanguage(request)
          val userInfo = displayUser(userid, uri, s"Create a $uri", lang)
          outputMainPage(
            create(uri, chooseLanguage(request),
              formSpecURI, makeAbsoluteURIForSaving(userid), copyRequest(request) ).getOrElse(<div/>),
            lang, userInfo=userInfo)
    }

  def backlinksAction(uri: String = "") = Action.async {
    implicit request: Request[_] =>
      val fut: Future[Elem] = backlinksFuture(uri)

      // create link for extended Search
      val extendedSearchLink = <p>
                                 <a href={ "/esearch?q=" + URLEncoder.encode(uri, "utf-8") }>
                                   Extended Search for &lt;{ uri }&gt;
                                 </a>
                               </p>
      fut.map { res =>
        val lang = chooseLanguage(request)
        outputMainPage(
          NodeSeq fromSeq Seq(extendedSearchLink, res), lang)
      }
  }

  def extSearch(q: String = "") = Action.async {
	  implicit request: Request[_] =>
	  val lang = chooseLanguage(request)
    val fut = esearchFuture(q)
    fut.map(r =>
    outputMainPage(r, lang))
  }

  //  implicit val myCustomCharset = Codec.javaSupported("utf-8") // does not seem to work :(

  def toolsPage = {
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val config1 = config
          val userInfo = displayUser(userid, "", "", lang)
          outputMainPage(
            new ToolsPage with ImplementationSettings.RDFModule with RDFPrefixes[ImplementationSettings.Rdf] {
              override val config: Configuration = config1
            }.getPage(lang, copyRequest(request)), lang, displaySearch = false, userInfo = userInfo)
            .as("text/html; charset=utf-8")

    }
  }

  def makeHistoryUserActionsAction(limit: String) =
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguage(request)
          val userInfo = displayUser(userid, "", "", lang)
          logger.info("makeHistoryUserActionsAction: cookies: " + request.cookies.mkString("; "))
          outputMainPage(makeHistoryUserActions(limit, lang, copyRequest(request) ), lang, userInfo = userInfo)
    }

}
