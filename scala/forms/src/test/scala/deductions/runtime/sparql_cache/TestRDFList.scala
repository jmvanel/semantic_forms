package deductions.runtime.sparql_cache

import org.scalatest.FunSuite
import deductions.runtime.utils.FileUtils
import org.w3.banana.SparqlOpsModule
import org.scalatest.Ignore
import org.scalatest.BeforeAndAfterAll
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.RDFCache
import org.w3.banana.jena.Jena
import org.w3.banana.binder.PGBinder
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import scala.util.Try

class TestRDFList extends FunSuite
    with JenaModule
    with RDFStoreLocalJena1Provider
   with TestRDFListTrait[Jena] {
}
   
trait TestRDFListTrait[Rdf <: RDF] extends FunSuite {

  implicit val ops: RDFOps[Rdf]
  import ops._
  type DATASET
  val dataset: DATASET
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]

  test("t1") {
    val newRdfsDomainsGraph = URI("g")
    val binder = PGBinder[Rdf, List[Int]]
    val list = List(1, 2)
    val listPg = binder.toPG(list)
    val graphToAdd = (
      URI("s") -- URI("p") ->-
      ( BNode() -- URI("p") ->- listPg.pointer)
        ).graph union listPg.graph
    println(s"graphToAdd $graphToAdd")
    rdfStore.appendToGraph(dataset, newRdfsDomainsGraph, graphToAdd)
  }
  
  test("t2") {
    val newRdfsDomainsGraph = URI("g")
    val binder = PGBinder[Rdf, List[Rdf#Node]]
    val list = List( URI("s"), URI("p") )
    val listPg = binder.toPG(list)
    val graphToAdd = (
      URI("s") -- URI("p") ->-
      ( BNode() -- URI("p") ->- listPg.pointer)
        ).graph union listPg.graph
    println(s"graphToAdd 2 $graphToAdd")
    println(s"graphToAdd 2.1 ${listPg.graph}")
    rdfStore.appendToGraph(dataset, newRdfsDomainsGraph, graphToAdd)
  }  
}