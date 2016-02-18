package deductions.runtime.stresstests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import io.gatling.core.scenario._

class BasicUseCasesSimulation extends Simulation {

  val httpConf = http.baseURL("http://localhost:9000")
  
  val chain =
        exec( http("wordsearch" ).get("/wordsearch?q=bidon") )
      . exec( http("displayuri henry").get("/display?displayuri=http%3A%2F%2Fbblfish.net%2Fpeople%2Fhenry%2Fcard%23me&Display=Afficher") )
      . exec( http("create").get("/create?uri=&uri=http%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2FPerson") )
      . exec( http("displayuri jmvanel").get("/display?displayuri=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me&Display=Afficher") )
      . exec( http("edit").get("/edit?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me") )

  val scn = scenario("Basic 1")
    .exec(http("").get("/"))
    .during(3 minutes) { chain }

  setUp(scn.inject(atOnceUsers(2)))
    .protocols(httpConf)
    
}

