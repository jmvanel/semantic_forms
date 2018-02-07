package deductions.runtime

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

/** See doc. http://www.scalatest.org/user_guide/using_selenium */
//class CRUDscenario extends Base with Basic with Login with CRUD {
class CRUDscenario extends Basic with Login with CRUD with Base {

  "The app home page" should "have the correct title" in homePageTest

  "The login page" should "bring to home page with user name present" in loginTest

  "The create page" should "create a Person with right first name" in createPersonTest
  
  Thread.sleep(10)
  webDriver.close()
}
