package deductions.runtime.html

import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.PrefixBuilder
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore.RDFStore2GraphStore
import org.w3.banana.diesel.toPointedGraphW
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.utils.Fil‍eUtils
import java.nio.file.Paths
import java.nio.file.Files

class TestCreationForm2 extends FunSuite with CreationForm {
  test("display form custom") ({
    Fil‍eUtils.deleteLocalSPARL()
    val uri = "http://xmlns.com/foaf/0.1/Person"
    val store =  RDFStoreObject.store
    retrieveURI( Ops.makeUri(uri), store )
    store.appendToGraph( Ops.makeUri("test"), graphTest.personFormSpec )
    val form = create(uri, lang="fr") 
    val file = "creation.form.2.html"
    Files.write(Paths.get(file), form.toString().getBytes );
    println( s"file created $file" )
    assert ( ! form . toString() . contains("homepage") )
    assert (   form . toString() . contains("firstName") )
    assert (   form . toString() . contains("lastName") )
  })

  
  trait GraphTest[Rdf <: RDF] {
    implicit val ops: RDFOps[Rdf] = Ops.asInstanceOf[org.w3.banana.RDFOps[Rdf]]
    import ops._
//    import syntax._
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
  
  val graphTest = new AnyRef with GraphTest[Rdf]
  println(TurtleWriter.asString(graphTest.personFormSpec, "" ))
}