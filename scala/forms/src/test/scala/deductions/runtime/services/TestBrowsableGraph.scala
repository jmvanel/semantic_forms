package deductions.runtime.services

import org.scalatest.FunSuite
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.SparqlHttpModule
import org.w3.banana.RDFOps
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFWriter
import org.w3.banana.RDFStore
import org.w3.banana.io.Turtle
import org.w3.banana.io.RDFXML
import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.SparqlOps
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.scalatest.Ignore
//import deductions.runtime.jena.JenaHelpers
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.RDFCache

//@Ignore
class TestBrowsableGraph
    extends FunSuite
    with RDFCache
    with RDFOpsModule
    with SparqlGraphModule
    with SparqlOpsModule
    with SparqlHttpModule
    with RDFStoreLocalJena1Provider
//    with JenaHelpers
    with BrowsableGraph[Jena, Dataset] {
  import ops._

  def test {
    lazy val bg = this // new BrowsableGraph[Rdf, Store] {}

    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    retrieveURI(makeUri(uri), dataset)
    println("bg.focusOnURI(uri)\n" + bg.focusOnURI(uri))
  }
}