package deductions.runtime.uri_classify

import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import SemanticURIGuesser._

class TestSemanticURIGuesser extends FunSuite {
  test("TestSemanticURIGuesser") {
    for (
      (uri, semanticType) <- List(
        ( "http://jmvanel.free.fr/images/jmv_id.jpg", Image ),
        ("http://danbri.org/foaf.rdf#danbri", SemanticURI)
        // fails because no clue except looking into content, or maybe using the presence of #   
//        ,("http://fcns.eu/people/andrei/card#me", SemanticURI)
    		  )
    ) {
      val fut = SemanticURIGuesser.guessSemanticURIType(uri)
      Await.result(fut, 1000000 millis)
      
      fut onSuccess{
        case t => println("SemanticURIType: "+t)
       	assert(t == semanticType)
      }
      fut onFailure {
        case t => println("onFailure SemanticURIType: "+t)
       	fail()
      }
    }
  }
}