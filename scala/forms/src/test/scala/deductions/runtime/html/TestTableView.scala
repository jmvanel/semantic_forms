package deductions.runtime.html

import org.scalatest.FunSuite

class TestTableView extends FunSuite with TableView {

  test("display form") {
//    val uri = "file:///home/jmv/jmvanel.free.fr/jmv.rdf"
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    val f = htmlFormString(uri)
    println(f)
  }
}