package deductions.runtime.html

import org.scalatest.FunSuite
import java.nio.file.Files
import java.nio.file.Paths

class TestTableView extends FunSuite with TableView {

  test("display form") {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val fo = htmlForm(uri, editable = true).get
    val f = TestCreationForm.wrapWithHTML(fo)
    Files.write(Paths.get("example.form.foaf.html"), f.toString().getBytes);
  }

  test("display form 2") {
    val uri = "http://dbpedia.org/resource/The_Lord_of_the_Rings"
    val fo = htmlForm(uri).get
    val f = TestCreationForm.wrapWithHTML(fo)
    Files.write(Paths.get("example.form.dbpedia.html"), f.toString().getBytes);
  }
}