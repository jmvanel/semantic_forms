import com.github.agourlay.cornichon.CornichonFeature

class SPARQL extends CornichonFeature {

import java.net.URLEncoder
lazy val query =
	"""
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         SELECT * WHERE {GRAPH ?G {?S ?P ?O .}} LIMIT 10
        """

    def feature = Feature("SPARQL") {
      Scenario("SPARQL SELECT") {
        When I get("http://localhost:9000/sparql?query=" + URLEncoder.encode(query) )
        Then assert status.is(200)
        And assert headers.name("Access-Control-Allow-Headers").isPresent
        And assert body.path("head.vars").containsString("G")
        And assert body.path("head.vars").containsString("O")
        And assert body.path("head.vars").containsString("P")
        And assert body.path("head.vars").containsString("S")
        And assert body.path("results.bindings").isPresent
        And assert body.path("results.bindings[0].G").isPresent
        And assert body.path("results.bindings[0].O").isPresent
        And assert body.path("results.bindings[0].P").isPresent
        And assert body.path("results.bindings[0].S").isPresent

      }
    }
  }


