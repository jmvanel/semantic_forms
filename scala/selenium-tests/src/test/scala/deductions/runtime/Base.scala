package deductions.runtime

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.selenium._

import org.openqa.selenium._
import org.openqa.selenium.firefox.FirefoxDriver
import java.util.concurrent.TimeUnit

import org.openqa.selenium.chrome.ChromeDriver

/** See doc. http://www.scalatest.org/user_guide/using_selenium */
trait Base {
  /*
   * Firefox:
   * https://stackoverflow.com/questions/38751525/firefox-browser-is-not-opening-with-selenium-webbrowser-code
   * get the driver here : https://github.com/mozilla/geckodriver/releases
   *
   * Chromium:
   * get the driver here :
   * https://sites.google.com/a/chromium.org/chromedriver/downloads
   */
  System.setProperty("webdriver.gecko.driver", "geckodriver");
  System.setProperty("webdriver.chrome.driver", "chromedriver");
  implicit val webDriver: WebDriver = {
    val wd =
      new FirefoxDriver
    // new ChromeDriver
    // new HtmlUnitDriver
    println(s"webDriver initialized: $wd")
    Thread.sleep(5000)
     wd.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    wd
  }

  lazy val host = "http://localhost:9000"

}
