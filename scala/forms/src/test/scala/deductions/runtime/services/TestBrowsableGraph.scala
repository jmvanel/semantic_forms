package deductions.runtime.services

import deductions.runtime.jena.{ImplementationSettings, RDFCache, RDFStoreLocalJenaProvider}
import deductions.runtime.utils.DefaultConfiguration
import org.scalatest.FunSuite
import org.w3.banana.{RDFOpsModule, SparqlGraphModule, SparqlHttpModule, SparqlOpsModule}
import deductions.runtime.sparql_cache.BrowsableGraph
//@Ignore
class TestBrowsableGraph
    extends FunSuite
    with RDFCache
    with RDFOpsModule
    with SparqlGraphModule
    with SparqlOpsModule
    with SparqlHttpModule
    with RDFStoreLocalJenaProvider
    with BrowsableGraph[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  import ops._

  def test() {
    lazy val bg = this // new BrowsableGraph[Rdf, Store] {}

    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    retrieveURI(makeUri(uri), dataset)
    println("bg.focusOnURI(uri)\n" + bg.focusOnURI(uri))
  }
}
