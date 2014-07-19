package deductions.runtime.jena

import org.scalatest.FunSuite
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.jena.JenaStore
import org.apache.log4j.Logger
import org.apache.jena.riot.RDFDataMgr

// deductions.runtime.jena.TestJenaRDFDataApp
object TestJenaRDFDataApp extends App with TestJenaRDFDataMgrRaw {
  test()
}

class TestJenaRDFDataMgr extends FunSuite with TestJenaRDFDataMgrRaw {
  test("JenaHelpers.storeURI") { test() }
}

trait TestJenaRDFDataMgrRaw extends JenaHelpers {

  def test() {
    val uri = Ops.makeUri("file:///home/jmv/ontologies/foaf.n3")
    val graphUri = Ops.makeUri("urn:foaf")
    lazy val dataset = TDBFactory.createDataset("TDB")
    lazy val store = JenaStore(dataset, defensiveCopy = false)
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri ")
      try {
        val gForStore = store.getGraph(graphUri)
//      val gForStore = store.addGraph(graphUri)
        RDFDataMgr.read(gForStore, uri.toString())
        Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      } catch {
        case t: Throwable => Logger.getRootLogger().error("ERROR: " + t)
      }
    }
    dataset.close
  }
}