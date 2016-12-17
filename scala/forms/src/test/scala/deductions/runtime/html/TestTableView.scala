package deductions.runtime.html

import java.nio.file.Files
import java.nio.file.Paths
import org.apache.log4j.Logger

import org.junit.Assert
import org.scalatest.Finders
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.utils.FileUtils
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings

class TestTableView extends FunSuite
    with ImplementationSettings.RDFModule
    with TableViewModule[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJena1Provider
    with BeforeAndAfter //    with DefaultConfiguration
    {
  val config = new DefaultConfiguration {}

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