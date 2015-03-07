package deductions.sparql

import org.w3.banana._
import scala.util.Try
import org.scalatest.FunSuite
import scala.concurrent.Future
import java.net.URL
import org.w3.banana.jena.JenaModule
import org.w3.banana.jena._
import com.hp.hpl.jena.query.Dataset
import com.hp.hpl.jena.query.DatasetFactory

abstract class TestSparqlGraphStore[Rdf <: RDF, M[+_], SE]
extends FunSuite
    with RDFOpsModule
    with SparqlOpsModule
    with TestFixtureRDF {
  val sEngine: SparqlQueryUpdateEngine[Rdf, M, SE]
  val se: SE
  val gs = new SparqlGraphStore[Rdf, M, SE](sEngine)
  import ops._

  test("display form") {

    val graphURI = makeUri("g")
    gs.appendToGraph(se, graphURI, graph)
    val res = gs.getGraph(se, graphURI)
    println(res)
    //    assert(???)
  }
}

class TestSparqlGraphStoreJena extends TestSparqlGraphStore[Jena, Try, Dataset] with JenaModule {

  val se = DatasetFactory.createMem()
  override val sEngine = new JenaDatasetStore(false).
    asInstanceOf[SparqlQueryUpdateEngine[Rdf, Try, Dataset]]
}