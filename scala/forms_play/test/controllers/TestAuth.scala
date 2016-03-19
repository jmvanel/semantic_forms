package controllers

import scala.concurrent.duration.DurationInt
import org.scalatest.FunSuite
import akka.util.Timeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers.contentAsString
import play.api.test.FakeApplication
import deductions.runtime.services.DefaultConfiguration
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.OneAppPerTest
import org.scalatestplus.play.OneAppPerSuite

class TestAuth
    extends PlaySpec
    with WhiteBoxTestdependencies
    with OneAppPerSuite
    with Secured
    with DefaultConfiguration {

  val loginName = // "http://jmvanel.free.fr/jmv.rdf#me" // 
    "devil@hell.com"
  val pw = "bla"
  val timeout: Timeout = Timeout(DurationInt(240) seconds)
  val auth = new AuthTrait {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
  }

  "Auth service" must {
    "implement signin" in {
      val request = FakeRequest(Helpers.POST,
        s"register?userid=${java.net.URLEncoder.encode(loginName, "utf-8")}" +
          s"&password=$pw&confirmPassword=$pw")
      val result = auth.register()(request)
      val content = contentAsString(result)(timeout)
      info(s"register: GET: contentAsString: ${synthetizeResult(content)}")
      val find_user = findUser(loginName)
      info(s"	findUser(loginName=$loginName): ${find_user}")
      assert(find_user.get.contains("devil"))
      // TODO check that a session has started
    }

    "implement login" in { login() }

    "implement logout" in {
      val request = FakeRequest(Helpers.GET, "logout")
      val result = auth.logout()(request)
      val content = contentAsString(result)(timeout)
      info(s"logout: GET: contentAsString: ${synthetizeResult(content)}")
    }
  }

  "implement re-login" in { login() }

  private def login() = {
    val request = FakeRequest(Helpers.GET,
      s"login?userid=$loginName,password=$pw")
    val result = auth.authenticate()(request)
    val content = contentAsString(result)(timeout)
    info(s"authenticate: GET: contentAsString: ${synthetizeResult(content)}")
  }

  private def synthetizeResult(content: String) = {
    content.split("\n").filter { _.contains("form") }.mkString("\n")
  }
}
