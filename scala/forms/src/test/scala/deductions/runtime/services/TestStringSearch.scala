package deductions.runtime.services

import org.w3.banana.RDFOpsModule
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCache
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._
import org.scalatest.FunSuite
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.RDF
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.BeforeAndAfterAll
import org.w3.banana.RDFXMLReaderModule
import deductions.runtime.utils.FileUtils

trait TestStringSearchTrait[Rdf <: RDF, DATASET] extends FunSuite
    with BeforeAndAfterAll
    with RDFOpsModule
    with RDFXMLReaderModule
    with StringSearchSPARQL[Rdf, DATASET] {

  /** populate TDB with a FOAF profile */
  override def beforeAll {
    import ops._
    rdfStore.rw(dataset, {
      val base = "http://jmvanel.free.fr/jmv.rdf"
      val uri = base + "#me"
      val from = new java.net.URL(base).openStream()
      val graph = rdfXMLReader.read(from, base).get
      rdfStore.appendToGraph(dataset, makeUri(uri), graph)
    })
  }

  override def afterAll {
    FileUtils.deleteLocalSPARL()
  }

  test("search 1") {
    val res = search("Jean")
    import scala.concurrent.ExecutionContext.Implicits.global
    res.onSuccess {
      case r =>
        println(r)
        assert(r.toString().contains("Jean-Marc"))
    }
    res.onFailure {
      case e =>
        println(e)
        fail(s"onFailure $e")
    }
    Await.ready(res, 2 seconds)
  }
}

class TestStringSearch extends FunSuite
  with TestStringSearchTrait[Jena, Dataset]
  with RDFStoreLocalJena1Provider
