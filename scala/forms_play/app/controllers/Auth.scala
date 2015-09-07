package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import deductions.runtime.services.Authentication
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalUserManagement
import com.hp.hpl.jena.query.Dataset


object Auth extends JenaModule
with RDFStoreLocalJena1Provider
with Auth[Jena, Dataset] {
//  println(s"object Auth")
}

trait Auth[Rdf <: RDF, DATASET]
extends ApplicationFacadeImpl[Rdf, DATASET]
 with Controller
 with RDFStoreLocalUserManagement[Rdf, DATASET] {
  
	/** checks user password in RDF database */
  val loginForm = Form(
    tuple("userid" -> text, "password" -> text)
      verifying
      ("Invalid userid or password",
        result => result match {
          case (userid, password) => checkLogin(userid, password)
        }))

  /** save user instance in RDF database */        
  val registerForm = Form(
    tuple("userid" -> text, "password" -> text, "confirmPassword" -> text)
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
    println( s"def login" )
    val lf = views.html.login(loginForm, registerForm)
    Ok(lf)
  }

  /** start a session after login if user Id & password are OK
   * this is the action of form `loginForm`;
   * actual recording in database declared in Form() registerForm
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.login(formWithErrors, registerForm)),
      user => {
      // Redirect to URL before login
        val previousURL = request.headers.get("referer")
        val call = previousURL match {
          case Some(url) => Call("GET", url)
          case None => routes.Application.index
        }
        Redirect(call).withSession(Security.username -> user._1)
      }
    )
  }

  /** start a session after registering user Id & password
   *  this is the action of form `registerForm`
   */
  def register = Action { implicit request =>
    val bfr = registerForm.bindFromRequest
    println(s"register = Action: bindFromRequest:\n\t$bfr")
    bfr.fold(
      formWithErrors => {
        println(s"register = Action: BadRequest:\n\t$bfr")
        BadRequest(views.html.login(loginForm, formWithErrors))
        },
        // TODO also Redirect to URL before login
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
