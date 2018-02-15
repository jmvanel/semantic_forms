package deductions.runtime

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait

//import org.openqa.selenium._

trait TestBoilerPlate extends FlatSpec with Matchers with WebBrowser {
  implicit val webDriver: WebDriver
  val host: String
  def webDriverWait = new WebDriverWait(webDriver, 15)

}