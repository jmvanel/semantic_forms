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
  println(s"object Auth")
}

trait Auth[Rdf <: RDF, DATASET]
extends ApplicationFacadeImpl[Rdf, DATASET]
 with Controller
 with RDFStoreLocalUserManagement[Rdf, DATASET] {
  
	/** checks user password in RDF database */
  val loginForm = Form(
    tuple("email" -> text, "password" -> text)
      verifying
      ("Invalid email or password",
        result => result match {
          case (email, password) => checkLogin(email, password) match {
            case Some(uri) => true
            case None => false
          }
        }))

  /** save user instance in RDF database */        
  val registerForm = Form(
    tuple("email" -> text, "password" -> text, "confirmPassword" -> text)
      verifying
      ("Passwords do not match",
        result => result match {
          case (email, password, confirmPassword) => password == confirmPassword
        })
        verifying
        ("User already exists",
          result => result match {
            case (email, password, confirmPassword) => true // ??
            //          val newUser = new User(email, password)
            //          newUser.save(newUser)
          }))

println(s"loginForm $loginForm")

  /** page for login or signin */
  def login = Action { implicit request =>
    println( s"def login" )
    val lf = views.html.login(loginForm, registerForm)
    println( s"lf : $lf" )
    Ok(lf)
//    Ok(views.html.login(loginForm, registerForm))
  }

  /** start a session after login if user Id & password are OK
   * this is the action of form `loginForm`
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, registerForm)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  /** start a session after registering user Id & password
   *  this is the action of form `registerForm`
   */
  def register = Action { implicit request =>
    registerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(loginForm, formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}
