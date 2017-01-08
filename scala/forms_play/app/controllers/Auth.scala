package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms
import play.api.data.Forms._
import deductions.runtime.services.Authentication

import org.w3.banana.RDF

import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings

import play.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.Lang
import play.api.i18n.I18nSupport
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

object Auth extends AuthTrait {
  val config = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }
}

trait AuthTrait
extends ImplementationSettings.RDFModule
with RDFStoreLocalJena1Provider
with Auth[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
//with DefaultConfiguration
{
  println(s"object Auth")
  /** NOTE otherwise we get "Lock obtain timed out", because
   *  LUCENE transactions would overlap with main database TDB/ */
}

/** Controller for registering account, login, logout;
 *  see https://www.playframework.com/documentation/2.4.x/ScalaSessionFlash */
trait Auth[Rdf <: RDF, DATASET]
extends ApplicationFacadeImpl[Rdf, DATASET]
 with Controller
 with RDFStoreLocalUserManagement[Rdf, DATASET] {
  
	/** checks user password in RDF database */
  val loginForm = Form(
    tuple("userid" -> Forms.text, "password" -> Forms.text)
      verifying
      ("Invalid userid or password",
        result => result match {
          case (userid, password) => checkLogin(userid, password)
        }))

  /** save user instance in RDF database */        
  val registerForm = Form(
    tuple("userid" -> Forms.text, "password" -> Forms.text, "confirmPassword" -> Forms.text)
      verifying
      ("Passwords do not match",
        result => result match {
          case (userid, password, confirmPassword) => password == confirmPassword
        })
        verifying
        (s"User already exists, or no email with this id",
          result => result match {
            case (userid, password, confirmPassword) =>
              println( s"""case $userid, $password, $confirmPassword""")
              val si = signin( userid, password )
              println( s"""For this ID <$userid> , URI associated is: "$si".""" )
              si.isSuccess
          }))

  println(s"loginForm $loginForm")

  /** page for login or signin */
  def login = Action { implicit request =>
    println( s"""def login request $request,
      cookies ${request.cookies}""" )
    val lf = views.html.login( loginForm, registerForm, redirect(request) )
    Ok("<!DOCTYPE html>\n" + lf)
    .as("text/html; charset=utf-8")
  }

  /** start a session after login if user Id & password are OK
   * this is the action of form `loginForm`;
   * actual recording in database declared in Form() registerForm
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest("<!DOCTYPE html>\n" + views.html.login(formWithErrors, registerForm))
                    .as("text/html; charset=utf-8"),
      user => {
        // Redirect to URL before login
        println(s"""authenticate: cookies ${request.cookies}
          get("to-redirect") ${request.session.get("to-redirect")}""")
        val previousURL = redirect(request)
        println(s"authenticate: previous url <$previousURL>")
        val call = previousURL match {
          case (url) if(
              url != "" &&
              ! url.endsWith("/login") &&
              ! url.endsWith("/authenticate") ) => Call("GET", url)
          case _ => routes.Application.index
        }
        Redirect(call).withSession(Security.username -> user._1)
      }
    )
  }

  def redirect(request: Request[_]) = request.session.get("to-redirect") . getOrElse(
        "" )

  /** start a session after registering user Id & password
   *  this is the action of form `registerForm`
   */
  def register = Action { implicit request =>
    val bfr = registerForm.bindFromRequest
    println(s"register = Action: bindFromRequest:\n\t$bfr")
    bfr.fold(
      formWithErrors => {
        println(s"register = Action: BadRequest:\n\t$bfr")
        BadRequest( // "<!DOCTYPE html>\n" + 
            views.html.login(loginForm, formWithErrors))
            .as("text/html; charset=utf-8")
        },
        // TODO also Redirect to the URL before login
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
          .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
    )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
