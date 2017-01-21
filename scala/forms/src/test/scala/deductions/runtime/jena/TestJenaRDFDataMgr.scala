package deductions.runtime.jena

import org.scalatest.FunSuite

import org.apache.log4j.Logger

import org.apache.jena.tdb.TDBFactory
import org.apache.jena.riot.RDFDataMgr
import org.scalatest.Ignore

object TestJenaRDFDataApp extends App with TestJenaRDFDataMgrRaw {
  test()
}

//@Ignore
class TestJenaRDFDataMgr extends FunSuite with TestJenaRDFDataMgrRaw {
  test("JenaHelpers.storeURI") { test() }
}

object JenaRDFDataMgrApp extends TestJenaRDFDataMgrRaw with App {
  readURL2(args(0))
}

trait TestJenaRDFDataMgrRaw extends ImplementationSettings.RDFModule {
  import ops._

  val graphUri = ops.makeUri("urn:foaf")

  def test() {
    val uri = "file:///home/jmv/ontologies/foaf.n3"
    readURL(uri)
  }

  def readURL2(uriString: String) {
    val g = RDFDataMgr.loadGraph("http://dbpedia.org/resource/Rome");
    println("size " + g.size());
  }

  def readURL(uriString: String) {
    val uri = makeUri(uriString)
    lazy val dataset = TDBFactory.createDataset("TDB_test")
    rdfStore.rw(dataset, {
      Logger.getRootLogger().info(s"storeURI uri $uri ")
      try {
        val gForStore = rdfStore.getGraph(dataset, graphUri).getOrElse(emptyGraph)
        //      val gForStore = store.addGraph(graphUri)
        RDFDataMgr.read(gForStore, uri.toString())
        Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      } catch {
        case t: Throwable => Logger.getRootLogger().error("ERROR: " + t)
      }
    })
    dataset.close
  }
}