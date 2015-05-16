package deductions.runtime.jena

import org.scalatest.FunSuite
import com.hp.hpl.jena.tdb.TDBFactory
import org.scalatest.Ignore
import org.w3.banana.jena.JenaModule
import java.io.File

object TestJenaHelpersApp extends App with TestJenaHelpersRaw {
  test()
}

//@Ignore
class TestJenaHelpers extends FunSuite with TestJenaHelpersRaw {
  test("JenaHelpers.storeURI") { test() }
}

trait TestJenaHelpersRaw
    extends JenaModule // JenaHelpers 
    {
  def test() {
    lazy val dataset1 = TDBFactory.createDataset("TDB")
    val jh = new JenaHelpers with RDFStoreLocalJenaProvider {
      val dataset: com.hp.hpl.jena.query.Dataset = dataset1
    }
    val uri = ops.makeUri(s"file://${new File(".").getAbsolutePath}/src/test/resources/foaf.n3")
    val graphUri = uri
    jh.storeURI(uri, graphUri, dataset1)
    //    store.executeSelect(query, bindings)))
  }
}