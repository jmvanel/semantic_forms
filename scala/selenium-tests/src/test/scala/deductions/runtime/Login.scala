import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

/** NOTE:
 *  It's not easy to set HTTP headers, so this is for browser set to french language
 *
 *  See ../web_tests/crud.selenium.xhtml ,
 *   doc. http://www.scalatest.org/user_guide/using_selenium */
class Login extends Base {
  val formsURIprefix = "http://raw.githubusercontent.com/jmvanel/semantic_forms/master/vocabulary/forms.owl.ttl#"
  "The login page" should "bring to home page with user name present" in {
    go to (host + "/login")
    val inputUserID = xpath(s" //input [ @data-rdf-property = '${formsURIprefix}userid' ]")
    click on inputUserID
    textField( inputUserID).value = "aa"
    val inputPW = xpath(s" //input [ @data-rdf-property = '${formsURIprefix}password' ]")
    click on inputPW
    pwdField( inputPW).value = "aa"
//    Thread.sleep(10)
    val inputAuth = xpath(" //input [ @formaction = '/authenticate' ]")
    click on inputAuth

    pageTitle should be ("Bienvenue à Semantic forms")
    println( linkText("user:aa") )
    assert( find(linkText("user:aa")).size == 1 )
  }
}
