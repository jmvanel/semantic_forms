import java.net.URLEncoder

import com.github.agourlay.cornichon.CornichonFeature

class FORM_DATA extends CornichonFeature {
  lazy val url =
    """
      http://jmvanel.free.fr/jmv.rdf#me
        """

  def feature = Feature("FORM_DATA") {

    Scenario("FORM_DATA") {

      get("http://localhost:9000/form-data").withParams(
        "displayuri" -> URLEncoder.encode(url)
      )
      Then assert status.is(200)
      And assert body.path("subject").isPresent
      //And assert body.path("form-label").is("Personne FOAF - court")
      And assert body.path("fields[0].subject").isPresent
      //And assert body.path("fields[0].value").is("Jean-Marc")

    }
  }
}