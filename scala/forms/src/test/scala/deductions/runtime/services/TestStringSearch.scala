package deductions.runtime.services

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Try
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.FunSuite
import org.w3.banana.RDF
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFXML
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.utils.FileUtils
import deductions.runtime.jena.LuceneIndex

trait TestStringSearchTrait[Rdf <: RDF, DATASET] extends FunSuite
    with BeforeAndAfterAll
    with RDFStoreLocalProvider[Rdf, DATASET]
    with StringSearchSPARQL[Rdf, DATASET] {

  val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]

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
    FileUtils.deleteLocalSPARQL()
  }

  test("search 1") {
    val res = searchString("Jean")
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
    Await.ready(res, 5 seconds)
  }
}

//@Ignore
class TestStringSearch extends FunSuite
  with RDFStoreLocalJena1Provider
  with TestStringSearchTrait[Jena, Dataset]
  with DefaultConfiguration
  with LuceneIndex