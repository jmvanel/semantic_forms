package deductions.runtime.sparql_cache

import deductions.runtime.jena.RDFStoreLocalJenaProvider
import deductions.runtime.utils.DefaultConfiguration
import org.scalatest.FunSuite
import org.w3.banana.{RDF, RDFOps, RDFStore}
import org.w3.banana.binder.PGBinder
import org.w3.banana.jena.{Jena, JenaModule}

import scala.util.Try

class TestRDFList extends FunSuite
    with JenaModule
    with RDFStoreLocalJenaProvider
    with TestRDFListTrait[Jena] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
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