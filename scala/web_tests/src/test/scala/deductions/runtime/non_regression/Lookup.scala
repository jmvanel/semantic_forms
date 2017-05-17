import com.github.agourlay.cornichon.CornichonFeature
import java.net.URLEncoder

class Lookup extends CornichonFeature {
  lazy val query =
    """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         SELECT * WHERE {GRAPH ?G {?S ?label ?O  .}} LIMIT 10
        """

  def feature = Feature("Lookup") {

    Scenario("Lookup") {

      When I get("http://localhost:9000/lookup?QueryString=" + URLEncoder.encode(query))
      Then assert status.is(200)

    }
  }
}