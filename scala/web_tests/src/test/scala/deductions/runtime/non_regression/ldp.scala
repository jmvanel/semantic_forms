import java.net.URLEncoder
import com.github.agourlay.cornichon.CornichonFeature

class LDP extends CornichonFeature {
  lazy val uri = "1496050844554-8066747071053"

  def feature = Feature("LDP") {

    Scenario("LDP") {
      RetryMax(5) {
        When I get("http://localhost:9000/ldp/" + uri)
        Then assert status.is(200)
        And assert headers.name("Access-Control-Allow-Origin").isPresent
        And assert body.path("@id").isPresent
      }
    }
  }
}
