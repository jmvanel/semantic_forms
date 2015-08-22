package deductions.runtime.html

import org.scalatest.FunSuite
import java.nio.file.Files
import java.nio.file.Paths
import org.scalatest.Ignore
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import org.w3.banana.jena.JenaModule
import org.junit.Assert

class TestTableView extends FunSuite
    with JenaModule
    with TableViewModule[Jena, Dataset]
    with RDFStoreLocalJena1Provider {

  lazy implicit val allNamedGraphs = allNamedGraph

  test("display form FOAF editable") {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val fo = htmlFormElem(uri, editable = true)
    val f = TestCreationForm.wrapWithHTML(fo)
    val result = f.toString()
    Assert.assertTrue("", result.contains("Jean-Marc"))
    Files.write(Paths.get("example.form.foaf.html"), result.getBytes)
  }

  test("display form dbpedia") {
    val uri = "http://dbpedia.org/resource/The_Lord_of_the_Rings"
    val fo = htmlFormElem(uri)
    val f = TestCreationForm.wrapWithHTML(fo)
    val result = f.toString()
    Assert.assertTrue("", result.contains("Tolkien"))
    Files.write(Paths.get("example.form.dbpedia.html"), result.getBytes);
  }
}