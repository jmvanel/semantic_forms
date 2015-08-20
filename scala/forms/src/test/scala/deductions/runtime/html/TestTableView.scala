package deductions.runtime.html

import org.scalatest.FunSuite
import java.nio.file.Files
import java.nio.file.Paths
import org.scalatest.Ignore
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset

// @Ignore
class TestTableView extends FunSuite
    with JenaHelpers
    with TableViewModule[Jena, Dataset]
    with RDFStoreLocalJena1Provider {

  test("display form") {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val fo = htmlFormElem(uri, editable = true)
    val f = TestCreationForm.wrapWithHTML(fo)
    Files.write(Paths.get("example.form.foaf.html"), f.toString().getBytes);
  }

  test("display form 2") {
    val uri = "http://dbpedia.org/resource/The_Lord_of_the_Rings"
    val fo = htmlFormElem(uri)
    val f = TestCreationForm.wrapWithHTML(fo)
    Files.write(Paths.get("example.form.dbpedia.html"), f.toString().getBytes);
  }
}