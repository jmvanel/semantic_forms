package deductions.runtime.scalawebtest

import org.scalawebtest.core.IntegrationFlatSpec
import org.scalawebtest.core.FormBasedLogin
import org.scalatest.AppendedClues
import org.scalawebtest.core.gauge.HtmlGauge
import org.scalatest.concurrent.PatienceConfiguration.Timeout

/** generic base trait for tests */
trait ScalaWebTestBaseSpec extends IntegrationFlatSpec
//  with FormBasedLogin
  with AppendedClues
  with HtmlGauge {

  override val host = "http://localhost:9000"
//  override val loginPath = "/login"
  override val projectRoot = ""
  //  override def loginTimeout = Timeout(5 seconds)
}

class CRUD
  extends ScalaWebTestBaseSpec {

  override val path = "/create"
  "When accessing protectedContent it" should "show the login form" in {
    // Déjà membre: se connecter
    fits(
//      <form id="form">
      <form >
        <input ></input>
        <input ></input>
      </form>)
  }

//  it should "hide the protected content, when not logged in" in {
//    not fit <p>sensitive information</p>
//  }

//  it should "show the protected content, after logging in" in {
//    textField("username").value = "admin"
//    pwdField("password").value = "secret"
//
//    submit()
//
//    fits(<p>sensitive information</p>)
//  }
}