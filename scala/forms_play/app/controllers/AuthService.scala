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
{

  import ops._

  /** page for login or signin */
  def login = Action { implicit request: Request[_] =>
    println( s"""login: request $request,
      cookies ${request.cookies}
      keySet ${request.session.data.keySet}""" )

    val formURI = forms("loginForm")
    val httpRequest = copyRequest(request)
//    retrieveURIBody(classURI, dataset, httpRequest, transactionsInside = true)

   val lfTry = 
     wrapInReadTransaction {
     htmlFormElemRaw(
       "tmp:login",
//       unionGraph: Rdf#Graph=allNamedGraph,
//       hrefPrefix: String = config.hrefDisplayPrefix,
//       blankNode: String = "",
    editable = true,
    actionURI = "/authenticate",
    lang = httpRequest.getLanguage(),
    graphURI = "",
    actionURI2 = "/authenticate",
//    formGroup = fromUri(nullURI),
    formuri= fromUri(formURI),
//    database = "TDB",
    request = httpRequest 
  ) . _1 // ( NodeSeq, FormSyntax )
    }
    val lf = lfTry  match {
      case Success(lf) => lf
      case Failure(f) => println (s"login: Error: $f")
      Text(s"login: Error: $f")
    }
    val content = <div>
                   Veuillez vous identifier afin d'accéder au système
                   <p/>
                   Déjà membre
                   <p/>
                   Se connecter
                   <p/>
                   Créer un compte
                   { lf }
               </div>

    val page = mainPage(content, userInfo = <div/>, lang  =  httpRequest.getLanguage(), title = "" )
      
    Ok("<!DOCTYPE html>\n" + page)
    .as("text/html; charset=utf-8")
  }

  /**
   * start a session after login if user Id & password are OK
   * this is the action of form `loginForm`;
   * actual recording in database declared in Form() registerForm
   */
  def authenticate = Action {
    implicit request: Request[_] =>
    val httpRequest = copyRequest(request)
    val userFromSession = httpRequest.userId() // normally not yet set in session !

    // TODO get triples from form (see FormSaver)
    
//    loginForm.bindFromRequest.fold(
//      formWithErrors =>
//        BadRequest("<!DOCTYPE html>\n" +
//            ??? // TODO
////            views.html.login(formWithErrors, registerForm))
//          .as("text/html; charset=utf-8"),
//      user => {

        // Redirect to URL before login
        println(s"""authenticate: cookies ${request.cookies}
          get("to-redirect") ${request.session.get("to-redirect")}
          keySet ${request.session.data.keySet}""")
            val previousURL = redirect(request)
            println(s"authenticate: previous url <$previousURL>")
            val call = previousURL match {
              case (url) if (
                url != "" &&
                !url.endsWith("/login") &&
                !url.endsWith("/authenticate")) => Call("GET", url)
              case _ => routes.Application.index
            }
            Redirect(call).withSession(Security.username ->
            ???
            // user._1
                )
              .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")

  }

  /** get the URL to redirect after authentification */
  def redirect(request: Request[_]) = request.session.get("to-redirect") . getOrElse("")

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
