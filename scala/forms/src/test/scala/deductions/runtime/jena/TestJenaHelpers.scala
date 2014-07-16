package deductions.runtime.jena

import org.scalatest.FunSuite
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.jena.JenaStore

object TestJenaHelpersApp extends App with TestJenaHelpersRaw {
  test()
}

class TestJenaHelpers extends FunSuite with TestJenaHelpersRaw {
  test("JenaHelpers.storeURI") { test() }
}

trait TestJenaHelpersRaw extends JenaHelpers {
  def test() {
    val jh = new JenaHelpers {}
    val uri = Ops.makeUri("src/test/resources/foaf.n3")
    val graphUri = uri
    lazy val dataset = TDBFactory.createDataset("TDB")
    lazy val store = JenaStore(dataset, defensiveCopy = false)
    jh.storeURI(uri, graphUri, store)
//    store.executeSelect(query, bindings)
  }
}