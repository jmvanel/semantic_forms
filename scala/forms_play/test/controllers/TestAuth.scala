package controllers

import play.api.test._
import play.api.test.Helpers._
import deductions.runtime.jena.RDFCache
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.TurtleWriterModule
import org.scalatest.FunSuite
import akka.util.Timeout
import scala.concurrent.duration._

class TestAuth extends FunSuite
    with // WhiteBoxTestdependencies 
    JenaModule
    with RDFStoreLocalJena1Provider
    with RDFOpsModule
    with SparqlGraphModule
    with TurtleWriterModule {

  val timeout: Timeout = Timeout(DurationInt(240) seconds)

  test("login") {
    val request = FakeRequest(Helpers.GET, "login")
    val result = controllers.Auth.login()(request)
    val content = contentAsString(result)(timeout)
    info("GET: contentAsString: " + content)
    //    assert(content.contains("Salut!"))
  }
}