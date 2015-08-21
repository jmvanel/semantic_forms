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
import org.scalatest.BeforeAndAfterAll
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
//import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import com.hp.hpl.jena.query.Dataset
import org.w3.banana.io.RDFWriter
import scala.util.Try
import org.w3.banana.io.Turtle
import org.w3.banana.RDFOps

class TestCreationForm2Jena extends FunSuite with TestForJena with TestCreationForm2[Jena, Dataset]

trait TestForJena extends JenaModule
//with JenaHelpers
  with RDFStoreLocalJena1Provider

/** Test Creation Form with form specification */
trait TestCreationForm2[Rdf <: RDF, DATASET] extends FunSuite
    //    with TurtleWriterModule
    with CreationFormAlgo[Rdf, DATASET]
    with GraphTest[Rdf]
    with BeforeAndAfterAll {

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]

  import ops._

  override def afterAll {
    FileUtils.deleteLocalSPARQL()
  }

  test("display form custom") {

    val uri = "http://xmlns.com/foaf/0.1/Person"
    retrieveURI(makeUri(uri), dataset)
    // NOTE: without form_specs/foaf.form.ttl
    rdfStore.rw(dataset, {
      rdfStore.appendToGraph(dataset, makeUri("test"), personFormSpec)
    })

    val form = create(uri, lang = "fr")
    val file = "creation.form.2.html"
    Files.write(Paths.get(file), form.toString().getBytes);
    println(s"file created $file")
    assert(!form.toString().contains("homepage"))
    assert(form.toString().contains("firstName"))
    assert(form.toString().contains("lastName"))
  }

  println(turtleWriter.asString(personFormSpec, "blabla"))
}

trait GraphTest[Rdf <: RDF] //extends RDFOpsModule
{
  implicit val ops: RDFOps[Rdf]

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
