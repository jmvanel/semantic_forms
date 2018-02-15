package deductions.runtime

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

/** See doc. http://www.scalatest.org/user_guide/using_selenium */
trait Basic extends TestBoilerPlate {

  def homePageTest = {
    println( s"homePageTest: webDriver: $webDriver")
    go to (host + "")
    pageTitle should be ("Bienvenue à Semantic forms")
  }

}
