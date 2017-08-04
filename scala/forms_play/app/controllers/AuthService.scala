package controllers

import deductions.runtime.services.Authentication
import org.w3.banana.RDF
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.mvc._

//import deductions.runtime.services.ApplicationFacade
import deductions.runtime.sparql_cache.dataset.RDFStoreLocalUserManagement
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import deductions.runtime.services.html.TriplesViewModule
import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.utils.Configuration
import scala.util.Success
import scala.util.Failure
import scala.xml.Text
import views.MainXmlWithHead
import deductions.runtime.services.FormSaver
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest
import scala.util.Try

// class
object AuthService extends AuthServiceTrait {
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
with ImplementationSettings.RDFCache // RDFStoreLocalJena1Provider
with AuthServiceTrait2[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  println(s"object Auth")
  /** NOTE otherwise we get "Lock obtain timed out", because
   *  LUCENE transactions would overlap with main database TDB/ */
}

/** Controller for registering account, login, logout;
 *  see https://www.playframework.com/documentation/2.4.x/ScalaSessionFlash */
trait AuthServiceTrait2[Rdf <: RDF, DATASET]
extends Controller
 with Authentication[Rdf, DATASET]
 with RDFStoreLocalUserManagement[Rdf, DATASET]
 with TriplesViewModule[Rdf, DATASET]
 with HTTPrequestHelpers
 with MainXmlWithHead
 with FormSaver[Rdf, DATASET] {

  import ops._

  val loginFormURI = forms("loginForm")
  val registerFormURI = forms("registerForm")

  val useridProp = form("userid") 
  val passwordProp = form("password")
  val confirmPasswordProp = form("confirmPassword")

  /** page for login or signin */
  def login = Action { implicit request: Request[_] =>
    println(s"""login: request $request,
      cookies ${request.cookies}
      keySet ${request.session.data.keySet}""")

    val httpRequest = copyRequest(request)

    val loginForm = {
      val lfTry =
        wrapInReadTransaction {
          htmlFormElemRaw(
            "tmp:login",
            editable = true,
            actionURI = "/authenticate2",
            lang = httpRequest.getLanguage(),
            graphURI = "",
            actionURI2 = "/authenticate2",
            formuri = fromUri(loginFormURI),
            request = httpRequest)._1
        }
      lfTry match {
        case Success(lf) => lf
        case Failure(f) =>
          val mess = s"login: Error: $f"
          println(mess)
          Text(mess)
      }
    }

    val registerForm = {
      val lfTry =
        wrapInReadTransaction {
          htmlFormElemRaw(
            "tmp:login",
            editable = true,
            actionURI = "/register2",
            lang = httpRequest.getLanguage(),
            graphURI = "",
            actionURI2 = "/register2",
            formuri = fromUri(registerFormURI),
            request = httpRequest)._1
        }
      lfTry match {
        case Success(lf) => lf
        case Failure(f) =>
          val mess = s"register: Error: $f"
          println(mess)
          Text(mess)
      }
    }

    val content = <div>
                    Veuillez vous identifier afin d'accéder au système<p/>
                    Déjà membre<p/>
                    Se connecter
    	{ loginForm }
                    <p/>
                    Créer un compte
      { registerForm }
    </div>

    val page = mainPage(content, userInfo = <div/>, lang = httpRequest.getLanguage(), title = "")

    Ok("<!DOCTYPE html>\n" + page)
      .as("text/html; charset=utf-8")
  }

  /**
   * start a session after login if user Id & password are OK */
  def authenticate = Action {
    implicit request: Request[_] =>
      val httpRequest = copyRequest(request)

      val userFromSession = httpRequest.userId() // normally not yet set in session !
println( s"""authenticate: httpRequest $httpRequest - queryString ${httpRequest.queryString}
    	userFromSession $userFromSession
    	formMap ${httpRequest.formMap}""" )

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
println( s"useridOption $useridOption") 
println( s"passwordOption $passwordOption" )

      if( loginChecked ) {
      // Redirect to URL before login
      println(s"""authenticate: cookies ${request.cookies}
          get("to-redirect") ${request.session.get("to-redirect")}
          keySet ${request.session.data.keySet}""")
      val previousURL = redirect(request)
      println(s"authenticate: previous url <$previousURL>")
      val call = previousURL match {
        case (url) if (
          url != "" &&
          !url.endsWith("/login2") &&
          !url.endsWith("/authenticate2")) => Call("GET", url)
        case _ => routes.Application.index
      }
      Redirect(call).withSession(Security.username -> useridOption.get )
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      } else {
         makeBadRequest( <div>login NOT Checked</div> )
      }
  }

  private def makeBadRequest(message: NodeSeq) = BadRequest(
    "<!DOCTYPE html>\n" +
      mainPage(message, userInfo = NodeSeq.Empty))
    .as("text/html; charset=utf-8")
          
  def decodeResponse(httpRequest: HTTPrequest) = {
    // get triples from form (see FormSaver)
    val trs = getTriplesFromHTTPrequest(httpRequest): Iterable[(Rdf#Triple, Seq[String])]
    val predicateToValue = for ((triple, values) <- trs) yield {
      triple.predicate(ops) -> values.headOption
    }
    val predicateToValueMap = predicateToValue.toMap
    println(s"predicateToValueMap $predicateToValueMap")

    val useridOption = predicateToValueMap.get(useridProp).flatten
    val passwordOption = predicateToValueMap.get(passwordProp).flatten
    val confirmPasswordOption = predicateToValueMap.get(confirmPasswordProp).flatten
    (useridOption, passwordOption, confirmPasswordOption)
  }

  /**
   * start a session after registering user Id & password
   *  this is the action of form `registerForm`
   */
  def register = Action {
    implicit request: Request[_] =>
      val httpRequest = copyRequest(request)
      println(s"register = Action: Request:\n\t$httpRequest")

      val (useridOption, passwordOption, confirmPasswordOption) = decodeResponse(httpRequest)

      val checkRegisterOption = for (
        userid <- useridOption;
        password <- passwordOption;
        confirmPassword <- confirmPasswordOption if (password == confirmPassword)
      ) yield signin(userid, password)

      val registerChecked = checkRegisterOption match {
        case Some(Success(id)) => true
        case _                 => false
      }

      if (registerChecked) {
        // TODO also Redirect to the URL before login
        println(s"register: user: $useridOption")
        Redirect(routes.Application.index).withSession(
          Security.username -> makeURIPartFromString(useridOption.get))
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
      } else {
        println(s"""register = Action: BadRequest:\n\t$useridOption,
          $passwordOption, $confirmPasswordOption""")
        makeBadRequest(<div>Register NOT succeeded for user {useridOption}</div>)
      }
  }

  /** get the URL to redirect after authentification */
  def redirect(request: Request[_]) = request.session.get("to-redirect") . getOrElse("")

  def logout = Action {
    Redirect(routes.AuthService.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
