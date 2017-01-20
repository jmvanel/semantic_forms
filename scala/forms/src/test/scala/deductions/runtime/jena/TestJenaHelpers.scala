package deductions.runtime.jena

import java.io.File

import org.apache.jena.tdb.TDBFactory
import org.scalatest.FunSuite
import org.w3.banana.jena.JenaModule

import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.RDFCacheAlgo

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
    val jh = new RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] with RDFStoreLocalJena1Provider {
      val config = new DefaultConfiguration {
        override val useTextQuery = false
      }
      override val databaseLocation = "TDB"
      //      val dataset: com.hp.hpl.jena.query.Dataset = dataset1
    }
    val uri = ops.makeUri(s"file://${new File(".").getAbsolutePath}/src/test/resources/foaf.n3")
    val graphUri = uri
    jh.readStoreURI(uri, graphUri, dataset1)
  }
}