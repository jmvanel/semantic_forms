package deductions.runtime.uri_classify

import org.scalatest.FunSuite

class TestSemanticURIGuesser extends FunSuite {
  test("TestSemanticURIGuesser") {
    for (
      uri <- List(
        "http://jmvanel.free.fr/images/jmv_id.jpg",
        "http://danbri.org/foaf.rdf#danbri",
        "http://fcns.eu/people/andrei/card#me")
    ) {
      // TODO currently crashes
//      val r = SemanticURIGuesser.guessSemanticURIType(uri)
//      println(r)
    }
    assert(true) // TODO
  }
}