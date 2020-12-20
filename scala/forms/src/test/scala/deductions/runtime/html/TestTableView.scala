package deductions.runtime.html

import java.nio.file.{Files, Paths}

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.services.html.{Form2HTMLObject, TriplesViewModule}
import deductions.runtime.utils.DefaultConfiguration
//import org.apache.log4j.Logger
import org.junit.Assert
import org.scalatest.{BeforeAndAfter, FunSuite}
import deductions.runtime.core.HTTPrequest
import com.typesafe.scalalogging.Logger
import deductions.runtime.utils.FormModuleBanana

class TestTableView extends FunSuite
    with ImplementationSettings.RDFModule
    with TriplesViewModule[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJenaProvider
    with FormModuleBanana[ImplementationSettings.Rdf]
    with BeforeAndAfter //    with DefaultConfiguration
    {
  val config = new DefaultConfiguration {}

  override val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

//  val logger = Logger("test")//.getRootLogger()
  lazy implicit val allNamedGraphs = allNamedGraph

  test("display form FOAF editable") {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val fo = htmlFormElem(uri, editable = true,
              request = HTTPrequest()
)
    val f = TestCreationForm.wrapWithHTML(fo)
    val result = f.toString()
    val correct = result.contains("Jean-Marc")
    //    if(correct)
    Files.write(Paths.get("example.form.foaf.html"), result.getBytes)
    Assert.assertTrue("""result.contains("Jean-Marc")""", correct)
  }

  before {
    println("!! before")
    println("empty Local SPARQL")
    rdfStore.rw(dataset, {
      dataset.asDatasetGraph().clear()
    })
    //    FileUtils.deleteLocalSPARQL()
  }

  //  test("display form dbpedia") {
  //    val uri = "http://dbpedia.org/resource/The_Lord_of_the_Rings"
  //    val fo = htmlFormElem(uri)
  //    val f = TestCreationForm.wrapWithHTML(fo)
  //    val result = f.toString()
  //    Assert.assertTrue("", result.contains("Tolkien"))
  //    Files.write(Paths.get("example.form.dbpedia.html"), result.getBytes);
  //  }
}