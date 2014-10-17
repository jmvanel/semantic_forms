package deductions.runtime.html
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreObject

class TestCreationForm extends FunSuite with CreationForm {
  test("display form") {
    val uri = // 
      "http://usefulinc.com/ns/doap#Project"
      // http://xmlns.com/foaf/0.1/Organization
      //       "http://xmlns.com/foaf/0.1/Person"
    val store =  RDFStoreObject.store
    retrieveURI( Ops.makeUri(uri), store )
    println( create(uri, lang="fr") )
    // TODO assertion
  }

}