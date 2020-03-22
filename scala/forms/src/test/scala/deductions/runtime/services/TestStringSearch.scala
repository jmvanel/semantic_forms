package deductions.runtime.services

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.jena.lucene.LuceneIndex
import deductions.runtime.utils.{DefaultConfiguration, FileUtils}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.w3.banana.RDF
import org.w3.banana.io.{RDFReader, RDFXML}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Try
import deductions.runtime.utils.FormModuleBanana

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
        // println(r.toString().)
        // assert(r.toString().contains("Jean-Marc"))
        assert(r.toString().contains("jmv.rdf#me"))
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
  with RDFStoreLocalJenaProvider
  with TestStringSearchTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
  with DefaultConfiguration
  with LuceneIndex
  with FormModuleBanana[ImplementationSettings.Rdf]{
    val config = new DefaultConfiguration{}
}