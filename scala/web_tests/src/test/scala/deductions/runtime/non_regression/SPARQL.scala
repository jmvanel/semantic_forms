import com.github.agourlay.cornichon.CornichonFeature

class ReadmeExample extends CornichonFeature {

/*
  def feature = Feature("OpenMovieDatabase API") {

    Scenario("list GOT season 1 episodes") {

      When I get("http://www.omdbapi.com").withParams(
        "t" -> "Game of Thrones",
        "Season" -> "1"
      )

      Then assert status.is(200)

      And assert body.ignoring("Episodes", "Response").is(
        """
        {
          "Title": "Game of Thrones",
          "Season": "1"
        }
        """)

      And assert body.path("Episodes").is(
        """
        |                Title                    |   Released   | Episode | imdbRating |   imdbID    |
        | "Winter Is Coming"                      | "2011-04-17" |   "1"   |    "8.1"   | "tt1480055" |
        | "The Kingsroad"                         | "2011-04-24" |   "2"   |    "7.8"   | "tt1668746" |
        | "Lord Snow"                             | "2011-05-01" |   "3"   |    "7.6"   | "tt1829962" |
        | "Cripples, Bastards, and Broken Things" | "2011-05-08" |   "4"   |    "7.7"   | "tt1829963" |
        | "The Wolf and the Lion"                 | "2011-05-15" |   "5"   |    "8.0"   | "tt1829964" |
        | "A Golden Crown"                        | "2011-05-22" |   "6"   |    "8.1"   | "tt1837862" |
        | "You Win or You Die"                    | "2011-05-29" |   "7"   |    "8.1"   | "tt1837863" |
        | "The Pointy End"                        | "2011-06-05" |   "8"   |    "7.9"   | "tt1837864" |
        | "Baelor"                                | "2011-06-12" |   "9"   |    "8.6"   | "tt1851398" |
        | "Fire and Blood"                        | "2011-06-19" |  "10"   |    "8.4"   | "tt1851397" |
        """)

      And assert body.path("Episodes").asArray.hasSize(10)

      And assert body.path("Episodes[0]").is(
        """
        {
          "Title": "Winter Is Coming",
          "Released": "2011-04-17",
          "Episode": "1",
          "imdbRating": "8.1",
          "imdbID": "tt1480055"
        }
        """)

      And assert body.path("Episodes[0].Released").is("2011-04-17")

      And assert body.path("Episodes").asArray.contains(
        """
        {
          "Title": "Winter Is Coming",
          "Released": "2011-04-17",
          "Episode": "1",
          "imdbRating": "8.1",
          "imdbID": "tt1480055"
        }
        """)
    }
  }

*/

import java.net.URLEncoder
lazy val query =
	"""
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         SELECT * WHERE {GRAPH ?G {?S ?label ?O  .}} LIMIT 10
        """
        // PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        // SELECT ?label WHERE {GRAPH ?G {?S ?label ?O  .}} LIMIT 10

    def feature = Feature("SPARQL") {
      Scenario("SPARQL SELECT") {
        When I get("http://localhost:9000/sparql?query=" + URLEncoder.encode(query) )
        Then assert status.is(200)
    }
  }
  }


