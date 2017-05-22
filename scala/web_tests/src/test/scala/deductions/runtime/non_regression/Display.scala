import java.net.URLEncoder
import com.github.agourlay.cornichon.CornichonFeature

class Display extends CornichonFeature {
  lazy val url =
    """
      http://jmvanel.free.fr/jmv.rdf#me
        """

  def feature = Feature("Display") {

      Scenario("Display") {
        RetryMax(5) {
        When I get("http://localhost:9000/display").withParams(
          "displayuri" -> URLEncoder.encode(url)
        )
          Then assert status.is(200)
          When i get("http://localhost:9000/download").withParams(
            "url" -> URLEncoder.encode(url)
          )
        Then assert status.is(200)
          When i get("http://localhost:9000/form-data").withParams(
            "displayuri" -> URLEncoder.encode(url)
          )
          And assert body.path("subject").isPresent
          Then assert status.is(200)
        }
      }
    }
  }
