import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

/** See doc. http://www.scalatest.org/user_guide/using_selenium */
class Basic extends Base {

  "The app home page" should "have the correct title" in {
    go to (host + "")
    pageTitle should be ("Bienvenue à Semantic forms")
  }

}
