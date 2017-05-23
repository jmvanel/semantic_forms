import java.net.URLEncoder
import com.github.agourlay.cornichon.CornichonFeature

class Form_data extends CornichonFeature {
  lazy val url =
    """
      http://jmvanel.free.fr/jmv.rdf#me
        """

  def feature = Feature("Form_data") {

    Scenario("Form_data") {
      RetryMax(5) {
        When I get("http://localhost:9000/form-data").withParams(
          "displayuri" -> URLEncoder.encode(url)
        )
        Then assert status.is(200)
        body.path("fields").isPresent
      }
    }
  }
}
