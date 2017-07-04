package deductions.runtime.html

import java.nio.file.{Files, Paths}

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJena1Provider}
import deductions.runtime.utils.DefaultConfiguration
import org.apache.log4j.Logger
import org.junit.Assert
import org.scalatest.{BeforeAndAfter, FunSuite}

class TestTableView extends FunSuite
    with ImplementationSettings.RDFModule
    with TriplesViewModule[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJena1Provider
    with BeforeAndAfter //    with DefaultConfiguration
    {
  val config = new DefaultConfiguration {}
//  val conf = config
//  val ops1 = ops
//  val htmlGenerator = new Form2HTMLBanana[ImplementationSettings.Rdf] {
//    implicit val ops = ops1
//    val config = conf
//    val nullURI = ops.URI("")
//  }
  override val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)

  val logger = Logger.getRootLogger()
  lazy implicit val allNamedGraphs = allNamedGraph

  test("display form FOAF editable") {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val fo = htmlFormElem(uri, editable = true)
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