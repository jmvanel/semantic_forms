package deductions.runtime.html

import java.nio.file.Files
import java.nio.file.Paths

import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.Prefix
import org.w3.banana.RDFOpsModule
import org.w3.banana.TurtleWriterModule
import org.w3.banana.diesel.toPointedGraphW
import org.w3.banana.jena.JenaModule

import deductions.runtime.utils.FileUtils

/** Test Creation Form with form specification */
class TestCreationForm2 extends FunSuite
    with CreationForm with TurtleWriterModule {
  println(ops.__xsdString) // TODO debug !!!!!!!!

  test("display form custom") {
    FileUtils.deleteLocalSPARL()
    val uri = "http://xmlns.com/foaf/0.1/Person"
    retrieveURI(ops.makeUri(uri), dataset)
    //    PopulateRDFCache.loadCommonFormSpecifications

    rdfStore.appendToGraph(dataset, ops.makeUri("test"), graphTest.personFormSpec)
    val form = create(uri, lang = "fr")
    val file = "creation.form.2.html"
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")
    assert(!form.toString().contains("homepage"))
    assert(form.toString().contains("firstName"))
    assert(form.toString().contains("lastName"))
  }

  val graphTest = new AnyRef with JenaModule with GraphTest
  println(turtleWriter.asString(graphTest.personFormSpec, "blabla"))
}

trait GraphTest extends RDFOpsModule {
  import ops._
  //  import syntax._
  println(ops) // TODO debug !!!!!!!!
  //    println( classOf[Rdf#URI] ) // debug !!!!!!!!
  val form = Prefix[Rdf]("form", "http://deductions-software.com/ontologies/forms.owl.ttl#")
  println(form) // TODO debug !!!!!!!!
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
