

/**
 * @author jmv
 */

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class LDPSpec extends Specification {

  val uri = "test1/"
  val file = "test1.ttl"
  val bodyTTL = """
    :s :p "Salut!".
    """
  "respond to the ldpPOST and ldp Actions" in {
    val request = FakeRequest("POST", "ldp/" + uri).
    withBody(bodyTTL).
    withHeaders(("Slug", file))
    val result0 = controllers.Application.ldpPOST(uri)(request)
    val result = result0.run
    
    status(result) must equalTo(OK)
    contentType(result) must beSome("text/plain")
    charset(result) must beSome("utf-8")
    contentAsString(result) must contain(uri)
  }

  "The 'Hello world' string" should {
    "contain 11 characters" in {
      "Hello world" must have size (11)
    }
  }
}