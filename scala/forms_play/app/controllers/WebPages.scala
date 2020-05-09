package controllers

import java.net.URLEncoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text

import deductions.runtime.html.TableView
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.html.Form2HTMLBanana
import deductions.runtime.services.html.HTML5TypesTrait
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.DefaultConfiguration
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.views.ToolsPage
import play.api.mvc.Action
import play.api.mvc.Request
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.core.SemanticController
import deductions.runtime.core.SemanticControllerFuture
import play.api.mvc.Result
import play.api.mvc.EssentialAction

import scalaz._
import Scalaz._
import deductions.runtime.views.FormHeader
import play.api.mvc.AnyContent
import play.api.mvc.RequestHeader
import deductions.runtime.core.SemanticControllerWrapper
import deductions.runtime.core.IPFilter
import deductions.runtime.utils.I18NMessages


object WebPagesApp extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  with WebPages
  with HTMLGenerator


/** controller for HTML pages ("generic application") */
trait WebPages extends PlaySettings.MyControllerBase
with ApplicationTrait
  with FormHeader[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with SemanticControllerWrapper {
  import config._

  private val ipFilterInstance = new IPFilter{}

  def index(): EssentialAction = {
    recoverFromOutOfMemoryErrorGeneric(
      {
        val contentMaker = new SemanticController {
          override def result(request: HTTPrequest): NodeSeq =
            makeHistoryUserActions("15", request)
        }
        outputMainPageWithContent(contentMaker)
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in landing page /index"))
  }

  /** no call of All Service Listeners */
  private case class MainPagePrecomputePlay(
      request: Request[_] ) {
    val requestCopy: HTTPrequest = copyRequest(request)
    // TODO copied below in MainPagePrecompute
    val lang = requestCopy.getLanguage()
    val userid = requestCopy.userId()
    val uri = expandOrUnchanged( requestCopy.getRDFsubject() )
    val title = labelForURITransaction(uri, lang)
    val userInfo = displayUser(userid, requestCopy.getRDFsubject(), title, lang)
  }

  /** side effet: call of All Service Listeners */
  private case class MainPagePrecompute(
      val requestCopy: HTTPrequest) {
    callAllServiceListeners(requestCopy)
    val lang = requestCopy.getLanguage()
    val userid = requestCopy.userId()
    val uri = expandOrUnchanged( requestCopy.getRDFsubject() )
    val title = labelForURITransaction(uri, lang)
    val userInfo = displayUser(userid, requestCopy.getRDFsubject(), title, lang)
    def this(request: Request[_]) = this(copyRequest(request))
  }

  /** output Main Page With given Content,
   * while filtering unwanted clients, @see IPFilter */
  private def outputMainPageWithContent(contentMaker: SemanticController, classForContent: String = "") = {
    Action { request0: Request[_] =>
      val precomputed = new MainPagePrecompute(request0)
      import precomputed._
      // println(s"========= outputMainPageWithContent precomputed $precomputed - title ${precomputed.title}")
      addAppMessageFromSession(requestCopy)
      outputMainPage2(
        contentMaker,
        precomputed, classForContent = classForContent)
    }
  }

  /** same as before, but Logging is enforced */
  private def outputMainPageWithContentLogged(
    contentMaker:    SemanticController,
    classForContent: String             // = "container sf-complete-form"
    ) = {
    withUser { implicit userid => implicit request =>
      val precomputed = new MainPagePrecompute(request)
      import precomputed._
      addAppMessageFromSession(requestCopy)
      // println(s"========= outputMainPageWithContentLogged precomputed $precomputed - title ${precomputed.title}")
      outputMainPage2(
        contentMaker,
        precomputed, classForContent = classForContent)
    }
  }

  def addAppMessageFromSession(requestCopy: HTTPrequest) = {
//    println( ">>>> addAppMessageFromSession session " + requestCopy.session )
//    println( ">>>> addAppMessageFromSession cookies " + requestCopy.cookies )
    val stringMess = requestCopy.flashCookie( "message" )
//    println( ">>>> addAppMessageFromSession stringMess " + stringMess )
    requestCopy.addAppMessage(
      <p>{ stringMess }</p>)
  }

  /** UNUSED in runtime ! */
  class ResultEnhanced(result: Result) {
    /** common HTTP headers for HTML */
    def addHttpHeaders(): Result = {
      result
        .withHeaders("Access-Control-Allow-Origin" -> "*") // for dbpedia lookup
        .as("text/html; charset=utf-8")
    }
    /** HTTP header Link rel='alternate' for declaring other RDF formats */
    def addHttpHeadersLinks(precomputed: MainPagePrecompute): Result = {
      addHttpHeadersLinks(precomputed.requestCopy.uri)
    }
    /** HTTP header Link rel='alternate' for declaring other RDF formats */
    def addHttpHeadersLinks(uri: String): Result = {
      val seq =
        for (
          (syntax, mime) <- Seq(
            ("Turtle", "application/turtle"),
            ("RDF/XML", "application/rdf+xml"),
            ("JSON-LD", "application/ld+json"))
        ) yield downloadURI(uri, syntax) + s"; rel='alternate'; type='$mime' , "
      result.withHeaders("Link" -> seq.mkString(""))
    }
  }
  import scala.language.implicitConversions
  /** UNUSED ! */
  private implicit def resultToResult(r: Result) = new ResultEnhanced(r)

  /** generate a Main Page wrapping given XHTML content;
   *  if HTTP URL contains &layout=form , do not apply SF application HTML header */
  private def outputMainPage( content: NodeSeq,
      userInfo: NodeSeq = <div/>, title: String = "",
      displaySearch:Boolean = true,
      classForContent: String ) // = "container sf-complete-form")
  (implicit request: Request[_]) : Result = {
    val httpRequest = copyRequest(request)
    val layout = httpRequest.getHTTPparameterValue("layout")
    httpWrapper(
      layout match {
        case Some("form") => content
        case _ =>
          mainPage( content,
            userInfo, title,
            displaySearch,
            messages = getDefaultAppMessage(),
            headExtra = getDefaultHeadExtra(),
            classForContent,
            httpRequest )
      } ,
      httpRequest )
  }

  private val DOCTYPE = """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
    "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
    """
//    """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
//	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
//	"""
  /** add HTML DOCTYPE and generic Headers, and HTTP 200 OK
   *  TODO use XML API to add DOCTYPE */
  private def httpWrapper( content: NodeSeq, httpRequest: HTTPrequest ) =
      Ok( DOCTYPE + content )
        .addHttpHeaders()
        .addHttpHeadersLinks( httpRequest.uri )

  /** generate a Main Page wrapping given XHTML content from a contentMaker,
   *  while filtering unwanted clients, @see IPFilter */
  private def outputMainPage2(
      contentMaker: SemanticController,
      precomputed:     MainPagePrecompute,
      displaySearch:   Boolean            = true,
      classForContent: String = "" // = "container sf-complete-form"
    ) : Result = {
    import precomputed._
    val layout = requestCopy.getHTTPparameterValue("layout")
    // Filtered content (blacklist, ...):
    val content = filterRequest2content( requestCopy,
      contentMaker, ipFilterInstance)
    httpWrapper(
      layout match {
        case Some("form") => content
        case _ => mainPage(
            content, userInfo, title,
            displaySearch,
            messages = getDefaultAppMessage(),
            headExtra = getDefaultHeadExtra(),
            classForContent,
            requestCopy
        ) },
      requestCopy )
  }

  /** generate a Main Page wrapping given XHTML content from a contentMaker,
   * while filtering unwanted clients, @see IPFilter;
   * Future version of the above
   */
  private def outputMainPageFuture(
      contentMaker: SemanticControllerFuture,
      precomputed:     MainPagePrecompute,
      displaySearch:   Boolean            = true,
      classForContent: String             = "" // "container sf-complete-form"
    )  : Future[Result] = {
    import precomputed._
    val layout = requestCopy.getHTTPparameterValue("layout")
    // Filtered content (blacklist, ...):
    val content = filterRequest2content( requestCopy,
      contentMaker, ipFilterInstance)
    content map { content =>
      httpWrapper(
      layout match {
        case Some("form") => content
        case _ => mainPage(
            content, userInfo, title,
            displaySearch,
            messages = getDefaultAppMessage(),
            headExtra = getDefaultHeadExtra(),
            classForContent,
            requestCopy
        ) },
      requestCopy )
    }
  }

  /**
   * (re)load & display URI
   *
   *  NOTE: parameters are just used by Play! to check presence of parameters,
   *  the HTTP request is analysed by case class MainPagePrecompute
   *
   *  @param Edit edit mode <==> param not ""
   */
  def displayURI(uri0: String, blanknode: String = "", Edit: String = "",
                 formuri: String = "") : EssentialAction
  = {
    recoverFromOutOfMemoryErrorGeneric(
      {
        val contentMaker = new SemanticController {
          override def result(request: HTTPrequest): NodeSeq = {
            val precomputed: MainPagePrecompute = MainPagePrecompute(request)
            import precomputed._
            logger.debug(s"displayURI: expandOrUnchanged <$uri>")
            val userInfo = displayUser(userid, uri, title, lang)
            htmlForm(uri, blanknode, editable = Edit  =/=  "", formuri,
              graphURI = makeAbsoluteURIForSaving(userid),
              request = request)._1
          }
        }

        // TODO decideLoginRequired(contentMaker)
        if (needLoginForDisplaying || (needLoginForEditing && Edit  =/=  ""))
          outputMainPageWithContentLogged(contentMaker, "")
        else
          outputMainPageWithContent(contentMaker)
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in /display URI"))
  }

  /** decide whether Login isRequired */
  private def decideLoginRequired(
      request: HTTPrequest,
    contentMaker: SemanticController, classForContent: String=""): EssentialAction =
    if (needLoginForDisplaying || isEditableFromRequest(request))
      outputMainPageWithContentLogged(contentMaker, classForContent)
    else
      outputMainPageWithContent(contentMaker, classForContent)


  /** table view, see makeClassTableButton() for a relevant SPARQL query
   *  IMPLEMENTATION: pipeline:
   *  URI --> query --> triples --> form syntax --> table cells */
  def table() = EssentialAction { implicit requestHeader: RequestHeader =>
    val request = copyRequestHeader(requestHeader)
    decideLoginRequired(
        request,
        tableContentMaker
    )(requestHeader)
  }

  private val tableContentMaker: SemanticController = new SemanticController {
    override def result(request: HTTPrequest): NodeSeq = {
      val query = queryFromRequest(request)
      val userid = request.userId()
      val title = "Table view from SPARQL"
      val lang = request.getLanguage
      val userInfo = displayUser(userid, "", title, lang)

      val editButton = <button action="/table" title="Edit each cell of the table (like a spreadheet)">Edit</button>
      val submitButton: NodeSeq /*Elem*/ =
        <button formaction="/save" formmethod="post" title="Save changes in the table">Submit</button>
      return  <div>
        <a href={
          request.uri.replaceFirst("/table", "/sparql-ui")
        }>{I18NMessages.get( "Edit_SPARQL_query", lang) }</a>
        </div> ++
        <form> {
            <input name="query" type="hidden" value={ query }></input> ++
              <input name="edit" type="hidden" value="yes"></input> ++
              <input type="hidden" name="graphURI" value={ makeAbsoluteURIForSaving(request.userId()) }/> ++
              {
                if (isEditableFromRequest(request))
                  submitButton
                else {
                  <!-- launch this table in edit mode -->
                  editButton
                }
              } ++
              tableFromSPARQL(request)
        } </form>
    }
  }

  /** TODO also elsewhere */
  def isEditableFromRequest(request: HTTPrequest): Boolean =
    request.queryString.getOrElse("edit", Seq()).headOption.getOrElse("") != ""

  /** generate an HTML table From SPARQL in request */
  private def tableFromSPARQL(request: HTTPrequest): NodeSeq = {
    logger.debug(s">>>> tableFromSPARQL: $request")
    val query = queryFromRequest(request)
    implicit val graph: Rdf#Graph = allNamedGraph
    // form Syntax With user Info
    val formSyntax = {
      val isEditable = isEditableFromRequest(request)
        val formSyntaxRaw = createFormFromSPARQL(query,
          editable = isEditable,
          formuri = "", request)
      if(isEditable)
        addUserInfoOnAllTriples(formSyntaxRaw)
      else
        formSyntaxRaw
    }
    val tableView = new TableView[ImplementationSettings.Rdf#Node, ImplementationSettings.Rdf#URI]
        with Form2HTMLBanana[ImplementationSettings.Rdf]
        with ImplementationSettings.RDFModule
        with HTML5TypesTrait[ImplementationSettings.Rdf]
        with RDFPrefixes[ImplementationSettings.Rdf]{
      val config = new DefaultConfiguration {}
      val nullURI = ops.URI("")
    }
    tableView.generate(formSyntax, request)
  }

  ///////////////// SPARQL related ////////////////////////

  private def queryFromRequest(request: HTTPrequest): String =
    request.queryString.getOrElse("query", Seq()).headOption.getOrElse("")

  /** /sparql-form service: Create HTML form or view from SPARQL (construct);
   *  like /sparql has input a SPARQL query;
   *  like /form and /display has parameters Edit, formuri & database
   */
  def sparqlForm(query: String, Edit: String = "", formuri: String = "",
                 database: String = "TDB") =
    Action { implicit request: Request[_] =>
      recoverFromOutOfMemoryErrorGeneric(
        {
          val requestCopy = getRequestCopy()
          val userid = requestCopy.userId()
          val lang = chooseLanguage(request)
          val userInfo = displayUser(userid, "", "", lang)
          outputMainPage(
            createHTMLFormFromSPARQL(
              query,
              editable = Edit  =/=  "",
              formuri, requestCopy),
            userInfo, classForContent="sf-complete-form")
        },
        (t: Throwable) =>
          errorResultFromThrowable(t, "in /sparql-form", request))
    }


  /** SPARQL Construct UI */
  def sparql(query: String) : EssentialAction = {
    logger.info("sparql: " + query)

    def doAction(implicit request: Request[_]) = {
      logger.info("sparql: " + request)
      val httpRequest = copyRequest(request)
      val lang = httpRequest.getLanguage()
      val userInfo = displayUser(getUsername(request).getOrElse("anonymous"), "pageURI", "title", "lang")
      outputMainPage(
          sparqlConstructQueryHTML(query, httpRequest, context=httpRequest.queryString2),
          userInfo=userInfo, classForContent="")
        // TODO factorize
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
    }

    recoverFromOutOfMemoryErrorGeneric(
      {
        if (needLoginForDisplaying)
          Action { implicit request: Request[_] => doAction }
        else
          withUser { implicit userid => implicit request => doAction }
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in SPARQL Construct UI /sparql-ui"))
  }

  /** SPARQL select UI */
  def select(query: String) =
    Action {
    implicit request: Request[_] =>
      recoverFromOutOfMemoryErrorGeneric(
        {
          logger.info("sparql: " + request)
          logger.info("sparql: " + query)
          val userInfo = displayUser(getUsername(request).getOrElse("anonymous"), "pageURI", "title", "lang")
          outputMainPage(
              selectSPARQL(query, copyRequest(request)),
                userInfo=userInfo, classForContent="" )
        },
        (t: Throwable) =>
          errorResultFromThrowable(t, "in SPARQL UI /select-ui", request))
    }


  /** search Or load+Display Action */
  def searchOrDisplayAction(q: String) = {
    def isURI(q: String): Boolean =
      // isAbsoluteURI(q)
      q.contains(":")
    
    if (isURI(q))
      displayURI( q, Edit="" )
    else
      wordsearchAction(q)
  }

  def wordsearchAction(q: String = "", clas: String = "") =
    boilerPlateActionFuture {
      precomputed =>
        import precomputed._
        logger.info(s"wordsearchAction: '$q' - IP ${requestCopy.remoteAddress}")
        recoverFromOutOfMemoryError(
          {
          val classe =
            clas match {
              case classe if (classe =/= "") => classe
              case _                         => requestCopy.getHTTPparameterValue("clas").getOrElse("")
            }
          wordsearchFuture(q, classe, requestCopy)
        },
          recoverFromOutOfMemoryErrorDefaultMessage(requestCopy.getLanguage()) +
            s", in word search /wordsearch?q=$q")
    }


  /** show Named Graphs */
  def showNamedGraphsAction() = boilerPlateActionFuture {
    precomputed =>
      import precomputed._
      requestCopy.
        setDefaultHTTPparameterValue("limit", "200").
        setDefaultHTTPparameterValue("offset", "1").
        setDefaultHTTPparameterValue("pattern", "")
      logger.info(s"showNamedGraphsAction: IP ${requestCopy.remoteAddress}")
      recoverFromOutOfMemoryError(showNamedGraphs(requestCopy))
  }

  /** boilerPlate for Action with Future;
   * generate a Main Page wrapping given XHTML content from a contentMaker,
   * while filtering unwanted clients, @see IPFilter */
  def boilerPlateActionFuture(sourceCode: MainPagePrecompute => Future[NodeSeq]) = {
    Action.async {
      implicit request: Request[_] =>
      val precomputed = new MainPagePrecompute(request)
        val contentMaker = new SemanticControllerFuture {
          override def result(request: HTTPrequest): Future[NodeSeq] = {
            sourceCode(precomputed)
          }
        }
        outputMainPageFuture(contentMaker, precomputed)
    }
  }

  /** show Triples In given Graph */
  def showTriplesInGraphAction(uri: String) = boilerPlateActionFuture {
    precomputed =>
      val lang = precomputed.requestCopy.getLanguage()
      recoverFromOutOfMemoryError(
        Future.successful(showTriplesInGraph(uri, lang)),
        s"in show Triples In Graph /showTriplesInGraph?uri=$uri")
  }

  /////////////////////////////////

  def edit(uri: String): EssentialAction =
    withUser { implicit userid => implicit request =>
      recoverFromOutOfMemoryErrorGeneric(
        {
          val lang = chooseLanguageObject(request).language
          val pageURI = uri
          val pageLabel = labelForURITransaction(uri, lang)
          val userInfo = displayUser(userid, pageURI, pageLabel, lang)
          logger.info(s"userInfo $userInfo, userid $userid")
          val httpRequest = copyRequest(request)
          val content = htmlForm(
            uri, editable = true,
            graphURI = makeAbsoluteURIForSaving(userid),
            request = httpRequest )._1
          httpWrapper(
             mainPage(content, userInfo, lang, httpRequest=httpRequest) ,
             httpRequest )
        },
        (t: Throwable) =>
          errorResultFromThrowable(t, "in /edit", request))
    }

  /**
   * save the HTML form;
   *  intranet mode (needLoginForEditing == false): no cookies session, just receive a `graph` HTTP param.
   *  TODO: this pattern should be followed for each page or service
   */
  def saveAction(): EssentialAction = {

    def saveLocal(userid: String)(implicit request: Request[_]) = {
      val httpRequest = copyRequest(request)
      logger.debug(s"""ApplicationTrait.saveOnly: class ${request.body.getClass},
              request $httpRequest""")
      val (uri, typeChanges) = saveOnly(
        httpRequest, userid, graphURI = makeAbsoluteURIForSaving(userid))
      logger.info(s"saveAction: uri <$uri>, typeChanges=$typeChanges")
      val saveAfterCreate = httpRequest.getHTTPheaderValue("Referer") .filter( _ .contains("/create?") ).isDefined
      val edit = typeChanges && ! saveAfterCreate
      val editParam = if( edit ) "edit" else ""
      val call = routes.WebPagesApp.displayURI(
        uri, Edit = editParam)
      Redirect(call).flashing(
        "message" ->
        s"The item <$uri> has been created" )
        // s"The item <$uri> of type <${httpRequest.getHTTPparameterValue("clas")}> has been created" )
      /* TODO */
      // recordForHistory( userid, request.remoteAddress, request.host )
    } // end saveLocal(

    recoverFromOutOfMemoryErrorGeneric(
      {
        if (needLoginForEditing)
          withUser { implicit userid => implicit request =>
            saveLocal(userid)
          }
        else
          Action { implicit request: Request[_] =>
            {
              val user = request.headers.toMap.getOrElse("graph", Seq("anonymous")).headOption.getOrElse("anonymous")
              saveLocal(user)
            }
          }
      },
      (t: Throwable) =>
        errorActionFromThrowable(t, "in save Actions /save"))
  }

  /** creation form - generic SF application */
  def createAction() =
    withUser { implicit userid => implicit request =>
      recoverFromOutOfMemoryErrorGeneric(
        {
          // URI of RDF class from which to create instance
          val uri0 = getFirstNonEmptyInMap(request.queryString, "uri")
          val uri = expandOrUnchanged(uri0)
          // URI of form Specification
          val formSpecURI = getFirstNonEmptyInMap(request.queryString, "formuri")
          logger.info(s"formSpecURI from HTTP request: <$formSpecURI>")
          val lang = chooseLanguage(request)
          outputMainPage(
            create(uri,
              formSpecURI, makeAbsoluteURIForSaving(userid), copyRequest(request)).getOrElse(<div/>),
            userInfo = displayUser(userid, uri, s"Create a $uri", lang), classForContent="" )
        },
        (t: Throwable) =>
          errorResultFromThrowable(t, "in create Actions /create", request))
    }

  /** Function for backlinks, asynchronous action */
  private val sourceCodeBacklinksAction: MainPagePrecompute => Future[NodeSeq] = {
    precomputed =>
      import precomputed._
      logger.info(s"backlinksAction: <$uri> - IP ${precomputed.requestCopy.remoteAddress}")
      val extendedSearchLink =
        <p>
          <a href={ "/esearch?q=" + URLEncoder.encode(uri, "utf-8") }>
            Extended Search for &lt;{ uri }
            &gt;
          </a>
        </p>
      val future: Future[NodeSeq] =
        recoverFromOutOfMemoryError(
          backlinksFuture(uri, requestCopy))
      future.map { formattedResults =>
        extendedSearchLink ++ formattedResults }
  }
  /** backlinks, asynchronous action */
  def backlinksAction(uriFromHTTP: String = "") =
    boilerPlateActionFuture {
      precomputed => sourceCodeBacklinksAction(precomputed)
    }

  def extSearch(q: String = "") =
    boilerPlateActionFuture {
      precomputed =>
        import precomputed._
        logger.info(s"extSearch: <${requestCopy.uri}> - IP ${requestCopy.remoteAddress}")
        recoverFromOutOfMemoryError(esearchFuture(q, requestCopy))
    }

  // implicit val myCustomCharset = Codec.javaSupported("utf-8") // does not seem to work :(

  def toolsPage = {
    withUser {
      implicit userid =>
        implicit request =>
          val lang = chooseLanguageObject(request).language
          val config1 = config
          val userInfo = displayUser(userid, "", "", lang)
          outputMainPage(
            new ToolsPage[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
            with ImplementationSettings.RDFCache
//            with RDFPrefixes[ImplementationSettings.Rdf]
          {
              override val config: Configuration = config1
            }.getPage( copyRequest(request)), displaySearch = false, userInfo = userInfo, classForContent="")
            .as("text/html; charset=utf-8")

    }
  }

  /** output Main Page With History of User Actions,
   * while filtering unwanted clients, @see IPFilter*/
  def makeHistoryUserActionsAction(limit: String): EssentialAction = {
//    request =>
    recoverFromOutOfMemoryErrorGeneric(
      {
      val contentMaker: SemanticController = new SemanticController {
        override def result(request: HTTPrequest): NodeSeq = {
          val precomputed: MainPagePrecompute = MainPagePrecompute(request)
          import precomputed._
          logger.info(s"makeHistoryUserActionsAction: ${request.logRequest()}, limit='$limit', cookies: ${request.cookies.mkString("; ")}" )
          makeHistoryUserActions(limit, request)
        }
      }
      outputMainPageWithContent(contentMaker)
    },
      (t: Throwable) =>
        errorActionFromThrowable(t,
            recoverFromOutOfMemoryErrorDefaultMessage("en" /*request.getLa*/) +
            s", in make History of User Actions /history?limit=$limit") )
  }

  def logRequest(httpRequest: HTTPrequest) {
    logger.info(httpRequest.logRequest)
  }
}
