package deductions.runtime.services

import org.scalatest.FunSuite
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlHttpModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.jena.Jena

import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.ImplementationSettings

//@Ignore
class TestBrowsableGraph
    extends FunSuite
    with RDFCache
    with RDFOpsModule
    with SparqlGraphModule
    with SparqlOpsModule
    with SparqlHttpModule
    with RDFStoreLocalJena1Provider
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
