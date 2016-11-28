package deductions.runtime.uri_classify

import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import SemanticURIGuesser._
import org.scalatest.Ignore
import scala.language.postfixOps

class TestSemanticURIGuesser extends FunSuite {
  test("TestSemanticURIGuesser") {
    for (
      (uri, semanticType) <- List(
        ("http://jmvanel.free.fr/images/jmv_id.jpg", Image),
        ("http://jmvanel.free.fr/images/jmv.rdf", SemanticURI),
        ("http://danbri.org/foaf.rdf#danbri", SemanticURI)

      // TODO: must try parent URI http://xmlns.com/foaf/0.1/ for a graph in RDF store
      //        , ("http://xmlns.com/foaf/0.1/Person", SemanticURI)

      // fails because no clue except looking into content, or maybe using the presence of #   
      //        , ("http://fcns.eu/people/andrei/card#me", SemanticURI)
      )
    ) {
      println(s"\nuri $uri")
      val fut = SemanticURIGuesser.guessSemanticURIType(uri)
      Await.result(fut, 10000 millis) // TODO avoid Await

      fut onSuccess {
        case t =>
          println("SemanticURIType: " + t)
          assert(t.toString() == semanticType.toString())
      }
      fut onFailure {
        case t =>
          println("onFailure SemanticURIType: " + t)
          fail()
      }
    }
  }
}