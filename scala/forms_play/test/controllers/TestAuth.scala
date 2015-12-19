package controllers

import scala.concurrent.duration.DurationInt
import org.scalatest.FunSuite
import akka.util.Timeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers.contentAsString
import play.api.test.FakeApplication

class TestAuth extends FunSuite
    //    with OneAppPerSuite
    with WhiteBoxTestdependencies
    with Secured {

  val loginName = "http://jmvanel.free.fr/jmv.rdf#me" // "devil@hell.com"
  val pw = "bla"
  val timeout: Timeout = Timeout(DurationInt(240) seconds)
    //  val fakeApplication = FakeApplication

  test("signin") {
    val request = FakeRequest(Helpers.POST,
        s"register?userid=${java.net.URLEncoder.encode(loginName, "utf-8")}&password=$pw&confirmPassword=$pw")
    val result = controllers.Auth.register()(request)
    val content = contentAsString(result)(timeout)
    //    info("GET: contentAsString: " + content)
    val find_user = findUser(loginName)
    info(s"	findUser(loginName=$loginName): ${find_user}")
    assert(find_user.get.contains("devil"))
    // TODO check that a session has started
  }
  
  test("login") {
    val request = FakeRequest(Helpers.GET,
        s"login?userid=$loginName,password=$pw")
    val result = controllers.Auth.login()(request)
    val content = contentAsString(result)(timeout)
    //    info("GET: contentAsString: " + content)
    //    assert(content.contains("Salut!"))
  }
}
