package controllers

import scala.util.Failure
import scala.util.Success
import scala.xml.NodeSeq
import scala.xml.Text

import org.w3.banana.RDF

import deductions.runtime.core.HTTPrequest
import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.Authentication
import deductions.runtime.services.FormSaver
import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.services.html.TriplesViewModule
import deductions.runtime.utils.RDFStoreLocalUserManagement
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.DefaultConfiguration
import deductions.runtime.views.MainXmlWithHead

import play.api.mvc.Action
import play.api.mvc.Call
import play.api.mvc.Request
import play.api.mvc.Security
// import play.api.mvc.Results._

import scalaz._
import Scalaz._
import deductions.runtime.utils.FormModuleBanana

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import play.api.mvc.BaseController

class AuthService @Inject() (val components: ControllerComponents)
extends AbstractController(components) with
AuthServiceTrait {
  override implicit val config: Configuration = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }
   override lazy val htmlGenerator:
    HtmlGeneratorInterface[ImplementationSettings.Rdf#Node, ImplementationSettings.Rdf#URI] =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
}

trait AuthServiceTrait
extends ImplementationSettings.RDFModule
with ImplementationSettings.RDFCache
with AuthServiceTrait2[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with FormModuleBanana[ImplementationSettings.Rdf] {
  logger.info(s"object Auth")
  /** NOTE otherwise we get "Lock obtain timed out", because
   *  LUCENE transactions would overlap with main database TDB/ */
}

/** Controller for registering account, login, logout;
 *  see https://www.playframework.com/documentation/2.4.x/ScalaSessionFlash */
trait AuthServiceTrait2[Rdf <: RDF, DATASET]
extends BaseController
 with Authentication[Rdf, DATASET]
 with RDFStoreLocalUserManagement[Rdf, DATASET]
 with TriplesViewModule[Rdf, DATASET]
 with HTTPrequestHelpers
 with MainXmlWithHead[Rdf, DATASET]
 with FormSaver[Rdf, DATASET] {

  import ops._
  /** do not log password in clear !!!!!!!!!!!! */
  val logPasswordInClear = false

  val loginFormURI = loginForms("loginForm")
  val registerFormURI = loginForms("registerForm")

  val useridProp = form("userid") 
  val passwordProp = form("password")
  val confirmPasswordProp = form("confirmPassword")

  /** page for login or signin */
  def login = Action { implicit request: Request[_] =>
    logger.info(s"""login: request $request,
      cookies ${request.cookies}
      keySet ${request.session.data.keySet}""")

    val httpRequest = copyRequest(request)

    val loginForm = {
          htmlFormElemRaw(
            "",
            editable = true,
            actionURI = "",
            graphURI = "",
            actionURI2 = "/authenticate",
            formuri = fromUri(loginFormURI),
            request = httpRequest)._1
    }

    val registerForm = {
          htmlFormElemRaw(
            "",
            editable = true,
            actionURI = "",
            graphURI = "",
            actionURI2 = "/register",
            formuri = fromUri(registerFormURI),
            request = httpRequest)._1
    }

    import deductions.runtime.utils.I18NMessages

    val content =
      <div>
        <p>Current user '{ httpRequest.userId()}'</p>
        <h3 id="login">{I18NMessages.get( "Login", httpRequest.getLanguage())}
          -
          <a href="#register" style="font-size: medium">{I18NMessages.get( "Create_account", httpRequest.getLanguage())}</a>
        </h3>
        { loginForm }
        <p/><br/>
        <h3 id="register" name="register">{I18NMessages.get( "Create_account", httpRequest.getLanguage())}</h3>
        { registerForm }
      </div>

    val page = mainPage(content, userInfo = <div/>, title = "",
        httpRequest=httpRequest)

    Ok("<!DOCTYPE html>\n" + page)
      .as("text/html; charset=utf-8")
  }

  /**
   * start a session after login if user Id & password are OK;
   * otherwise redirect to /login */
  def authenticate = Action {
    implicit request: Request[_] =>
      val httpRequest = copyRequest(request)

      val userFromSession = httpRequest.userId() // normally not yet set in session !
      if (logPasswordInClear)
        logger.info(s"""authenticate: httpRequest $httpRequest - queryString ${httpRequest.queryString}
          userFromSession $userFromSession
          formMap ${httpRequest.formMap}""")
      else
        logger.info( s"""authenticate: httpRequest host ${httpRequest.host}
          userFromSession '$userFromSession'""")

      val (useridOption, passwordOption, confirmPasswordOption)
        = decodeResponse(httpRequest)

      val checkLoginOption = for (
        userid <- useridOption;
        password <- passwordOption
      ) yield checkLogin(userid, password)

      val loginChecked = checkLoginOption match {
        case Some(true) => true
        case _          => false
      }
      if(logPasswordInClear)
        logger.info( s"useridOption $useridOption, passwordOption $passwordOption" )

      val message=""
      if (loginChecked) {
        // Redirect to URL before login
        logger.info(s"""authenticate: cookies ${request.cookies}
          get("to-redirect") ${request.session.get("to-redirect")}
          keySet ${request.session.data.keySet}""")
        val previousURL = redirect(request)
        logger.info(s"authenticate: previous url <$previousURL>")
        val call = previousURL match {
          case (url) if (
            url =/= "" &&
            !url.endsWith("/login") &&
            !url.endsWith("/authenticate")) => Call("GET", url)
          case _ => routes.WebPagesApp.index
        }

        Redirect(call).withSession("username" -> useridOption.get)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")

      } else {
//        Redirect(Call("GET", s"/login?message=$message"))
        logger.debug(s"Login failed for user '$useridOption'  Redirect /login")
        Redirect(Call("GET", "/login"))
        //         makeBadRequest( <div>login NOT Checked</div> )
      }
  }

  private def makeBadRequest(message: NodeSeq) = BadRequest(
    "<!DOCTYPE html>\n" +
      mainPage(message, userInfo = NodeSeq.Empty, httpRequest=HTTPrequest() ))
    .as("text/html; charset=utf-8")
          
  def decodeResponse(httpRequest: HTTPrequest) = {
    // get triples from form (see FormSaver)
    val trs = getTriplesFromHTTPrequest(httpRequest): Iterable[(Rdf#Triple, Seq[String])]
    val predicateToValue = for ((triple, values) <- trs) yield {
      triple.predicate(ops) -> values.headOption
    }
    val predicateToValueMap = predicateToValue.toMap
    if( logPasswordInClear)
      logger.info(s"decodeResponse: predicateToValueMap $predicateToValueMap")

    val useridOption = predicateToValueMap.get(useridProp).flatten
    val passwordOption = predicateToValueMap.get(passwordProp).flatten
    val confirmPasswordOption = predicateToValueMap.get(confirmPasswordProp).flatten
    (useridOption, passwordOption, confirmPasswordOption)
  }

  /**
   * start a session after registering user with Id & password
   *  this is the action of form `form:registerForm`
   */
  def register = Action {
    implicit request: Request[_] =>
      val httpRequest = copyRequest(request)

      val (useridOption, passwordOption, confirmPasswordOption) = decodeResponse(httpRequest)
      val userFromSession = httpRequest.userId() // normally not yet set in session !
      val givenLoginMatchesUserFromSession = useridOption.getOrElse("") === userFromSession
      val userRedefinesPassword = givenLoginMatchesUserFromSession && userFromSession != "anonymous"

      logger.info(s"""register ${httpRequest.logRequest()}
        useridOption $useridOption""")
      if(logPasswordInClear)
        print(s"\tpasswordOption $passwordOption, confirmPasswordOption $confirmPasswordOption")

        val checkRegisterOption = for (
        userid <- useridOption;
        password <- passwordOption;
        confirmPassword <- confirmPasswordOption;
        passwordConfirmed = (password === confirmPassword);
        useridLongEnough = (userid.length() >= 2);
        _ = logger.info( s"""\tpasswordConfirmed $passwordConfirmed, useridLongEnough $useridLongEnough
            givenLoginMatchesUserFromSession $givenLoginMatchesUserFromSession
            userRedefinesPassword $userRedefinesPassword""");
        if (passwordConfirmed && useridLongEnough &&
            (givenLoginMatchesUserFromSession || userFromSession == "anonymous") )
      ) yield signin(userid, password)

      val registerChecked = checkRegisterOption match {
        case Some(Success(id)) => true
        case _                 => false
      }

      val message= if(userRedefinesPassword) "user Redefines Password" else ""

      if (registerChecked) {
        // TODO also Redirect to the URL before login
        logger.info(s"register SUCCEEDED: user: $useridOption")
        Redirect(s"/").withSession(
//        Redirect(s"/?message=$message").withSession(
          "username" -> makeURIPartFromString(useridOption.get))
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      } else {
        logger.info(s"""register FAILED \n\t$useridOption
          passwordOption ${passwordOption.isDefined}, confirmPasswordOption ${confirmPasswordOption.isDefined}""" )
        if( logPasswordInClear)
          logger.info(s"\tpasswordOption $passwordOption, confirmPasswordOption $confirmPasswordOption")
        makeBadRequest(<div>Register NOT succeeded for user {useridOption}
          - user From Session {userFromSession}
          - givenLoginMatchesUserFromSession {givenLoginMatchesUserFromSession}
          - {message}
        </div>)
      }
  }

  /** get the URL to redirect after authentification */
  def redirect(request: Request[_]) = request.session.get("to-redirect") . getOrElse("")

  def logout = Action {
    // Redirect(routes.AuthService.login).withNewSession.flashing(
    Redirect( "/login" ).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
