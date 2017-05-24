import java.net.URLEncoder
import com.github.agourlay.cornichon.CornichonFeature

class Download extends CornichonFeature {
  lazy val url =
    """
      http://jmvanel.free.fr/jmv.rdf#me
        """

  def feature = Feature("Download") {

    Scenario("Download") {
      RetryMax(5) {
        When I get("http://localhost:9000/download").withParams(
          "url" -> URLEncoder.encode(url)
        )
        Then assert status.is(200)
        And assert headers.name("Access-Control-Allow-Origin").isPresent

      }
    }
  }
}
