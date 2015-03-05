package deductions.runtime.html

import java.nio.file.Files
import java.nio.file.Paths

import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.Prefix
import org.w3.banana.RDFOpsModule
import org.w3.banana.TurtleWriterModule
import org.w3.banana.diesel._
import org.w3.banana.jena.JenaModule

import deductions.runtime.utils.FileUtils

/** Test Creation Form with form specification */
class TestCreationForm2 extends FunSuite
    with CreationForm with TurtleWriterModule {

  import ops._
  
  test("display form custom") {
    FileUtils.deleteLocalSPARL()

    val uri = "http://xmlns.com/foaf/0.1/Person"
    retrieveURI(makeUri(uri), dataset)
    // NOTE: without form_specs/foaf.form.ttl
    rdfStore.rw( dataset, {
    	rdfStore.appendToGraph(dataset, makeUri("test"), graphTest.personFormSpec) })

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
  val form = Prefix[Rdf]("form", "http://deductions-software.com/ontologies/forms.owl.ttl#")
  val foaf = FOAFPrefix[Rdf]

  val personFormSpec0 = (
    URI("personForm")
    -- form("classDomain") ->- foaf.Person
    -- form("showProperties") ->- ( // list
      bnode("p1") -- rdf.first ->- foaf.firstName
      -- rdf.rest ->- (
        bnode("p2") -- rdf.first ->- foaf.lastName
        -- rdf.rest ->- rdf.nil))).graph

  val personFormSpec = (
    URI("personForm")
    -- form("classDomain") ->- foaf.Person
    -- form("showProperties") ->- List(
      foaf.firstName,
      foaf.lastName,
      foaf.topic_interest
    )).graph
}
