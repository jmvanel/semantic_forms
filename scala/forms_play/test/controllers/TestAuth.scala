package controllers

import akka.util.Timeout
import deductions.runtime.html.{Form2HTMLBanana, Form2HTMLObject}
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.contentAsString

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class TestAuth
    extends PlaySpec
    with DefaultConfiguration
    with WhiteBoxTestdependencies
    with OneAppPerSuite
    with Secured
    {

  lazy val config = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }

  lazy val htmlGenerator: Form2HTMLBanana[ImplementationSettings.Rdf] =
    		      Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

  val loginName = // "http://jmvanel.free.fr/jmv.rdf#me" // 
    "devil@hell.com"
  val pw = "bla"
  val timeout: Timeout = Timeout(DurationInt(240) seconds)
  val config1 = config
  val auth = new AuthTrait {
    val config = config1
    		val htmlGenerator: Form2HTMLBanana[ImplementationSettings.Rdf] =
    		      Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
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
