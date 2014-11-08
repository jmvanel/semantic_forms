package deductions.runtime.html
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.PrefixBuilder
import org.w3.banana.GraphStore
import org.w3.banana.RDF
import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.diesel._

class TestCreationForm2 extends FunSuite with CreationForm {
  test("display form custom") {
    val uri = "http://xmlns.com/foaf/0.1/Person"
    val store =  RDFStoreObject.store
    retrieveURI( Ops.makeUri(uri), store )
    val formp = new PrefixBuilder("form", "http://deductions-software.com/ontologies/forms.owl.ttl#" )
    val fo = formp("showProperties")
    store.appendToGraph( Ops.makeUri("test"), v.personFormSpec )
    val form = create(uri, lang="fr") 
    println( form )
    assert ( form . toString() . contains("homepage") )
  }

  trait GraphTest[Rdf <: RDF] {
    implicit val ops: RDFOps[Rdf] = Ops.asInstanceOf[org.w3.banana.RDFOps[Rdf]]
    import ops._
    import syntax._
    val form = new PrefixBuilder[Rdf]("form", "http://deductions-software.com/ontologies/forms.owl.ttl#")
    val foaf = FOAFPrefix[Rdf]
    val personFormSpec = (
      URI("personForm")
      -- form("classDomain") ->- foaf.Person
      -- form("showProperties") ->- ( // list
        bnode("p1") -- rdf.first ->- foaf.firstName
        -- rdf.rest ->- (
          bnode("p2") -- rdf.first ->- foaf.lastName
          -- rdf.rest ->- rdf.nil))).graph
  }
  
  val v = new AnyRef // ( implicit val ops : RDFOps[Rdf] )
  with GraphTest[Rdf]
//  (ops)
  println( "personFormSpec\n" + v.personFormSpec )
}