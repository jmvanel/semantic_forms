package controllers.stresstests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class MySimulation extends Simulation {

  val conf = http.baseUrl("http://localhost:9000")

  val scn = scenario("Basic 1")
    .exec(http("").get("/"))
    .during(3 minutes) {
      exec( http("").get("/wordsearch?q=bidon")
      exec( http("").get("/display?displayuri=http%3A%2F%2Fbblfish.net%2Fpeople%2Fhenry%2Fcard%23me&Display=Afficher")
      exec( http("").get("/create?uri=&uri=http%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2FPerson")
      exec( http("").get("/display?displayuri=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me&Display=Afficher")
      exec( http("").get("/edit?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me")
      // exec( http("").get("")
      )
    }

  setUp(scn.inject(atOnceUsers(2)))
    .protocols(conf)
}
