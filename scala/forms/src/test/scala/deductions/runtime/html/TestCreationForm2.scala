package deductions.runtime.html

import java.nio.file.{Files, Paths}

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.services.html.{CreationFormAlgo, Form2HTMLObject}
import deductions.runtime.utils.{DefaultConfiguration, FileUtils, RDFPrefixes}
//import org.apache.log4j.Logger
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.w3.banana.{RDF, RDFOps}
import org.w3.banana.io.{RDFWriter, Turtle}
import org.w3.banana.jena.JenaModule

import scala.util.Try
import deductions.runtime.core.HTTPrequest
import com.typesafe.scalalogging.Logger
import deductions.runtime.utils.FormModuleBanana

class TestCreationForm2Jena extends {
    override val config = new DefaultConfiguration{}
}
with FunSuite with TestForJena
with TestCreationForm2[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with FormModuleBanana[ImplementationSettings.Rdf]

trait TestForJena extends JenaModule
  with RDFStoreLocalJenaProvider

/** Test Creation Form with form specification */
trait TestCreationForm2[Rdf <: RDF, DATASET] extends FunSuite
    with CreationFormAlgo[Rdf, DATASET]
    with GraphTest[Rdf]
    with BeforeAndAfterAll {

	override lazy val htmlGenerator =
			Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
  
	  
  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]

//  val logger = Logger("test")//.getRootLogger()
  import ops._

 /** CAUTION: BeforeAndAfterAll.afterAll DOES NOT WORK directly with FunSuite:
  * afterAll runs after EACH test */
  override def afterAll {
    close()
    //  FileUtils.deleteLocalSPARQL()
  }

  test("display form custom") ({
    val classe = foaf("Person")
    val uri = classe.getString
    retrieveURI( classe, dataset)
    // NOTE: without form_specs/foaf.form.ttl
    rdfStore.rw(dataset, {
      rdfStore.appendToGraph(dataset, makeUri("test"), personFormSpec)
    })
    val form0 = create(uri, request = HTTPrequest(acceptLanguages = Seq("fr") ))
    val form = form0.get
    val file = "creation.form.2.html"
    val formString = form.toString()
    Files.write(Paths.get(file), formString.getBytes);
    println(s"file created $file")
    assert(formString.contains("firstName"))
    assert(formString.contains("lastName"))
    // should not contain field homepage, because foaf:homepage rdfs:domain owl:Thing .
    assert(!formString.contains(">homepage<"))
  })

  println("personFormSpec")
  println(turtleWriter.asString(personFormSpec, "base"))
}

trait GraphTest[Rdf <: RDF] extends RDFPrefixes[Rdf] with DefaultConfiguration
{
  implicit val ops: RDFOps[Rdf]

  import ops._

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
