package deductions.runtime.jena

import org.scalatest.FunSuite
import com.hp.hpl.jena.tdb.TDBFactory
import org.scalatest.Ignore

object TestJenaHelpersApp extends App with TestJenaHelpersRaw {
  test()
}

//@Ignore
class TestJenaHelpers extends FunSuite with TestJenaHelpersRaw {
  test("JenaHelpers.storeURI") { test() }
}

trait TestJenaHelpersRaw extends JenaHelpers {
  def test() {
    val jh = new JenaHelpers {}
    val uri = ops.makeUri("src/test/resources/foaf.n3")
    val graphUri = uri
    lazy val dataset = TDBFactory.createDataset("TDB")
    jh.storeURI(uri, graphUri, dataset)
    //    store.executeSelect(query, bindings)
  }
}