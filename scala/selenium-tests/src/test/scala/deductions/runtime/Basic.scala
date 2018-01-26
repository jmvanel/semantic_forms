import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver

/** See doc. http://www.scalatest.org/user_guide/using_selenium */
class Basic extends FlatSpec with Matchers with WebBrowser {

  /* jmv:
   * https://stackoverflow.com/questions/38751525/firefox-browser-is-not-opening-with-selenium-webbrowser-code
   * get the driver here : https://github.com/mozilla/geckodriver/releases
   */
  System.setProperty("webdriver.gecko.driver", "geckodriver");
  implicit val webDriver: WebDriver = new // ChromeDriver //
  FirefoxDriver // HtmlUnitDriver

  val host = "http://localhost:9000/"

  "The app home page" should "have the correct title" in {
    go to (host + "")
    pageTitle should be ("Bienvenue à Semantic forms")
  }
}
