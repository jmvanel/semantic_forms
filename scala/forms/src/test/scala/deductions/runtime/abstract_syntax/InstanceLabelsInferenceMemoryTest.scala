package deductions.runtime.abstract_syntax

import org.scalatest.FunSuite
import org.w3.banana.jena.JenaModule

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.RDFCacheAlgo

class InstanceLabelsInferenceMemoryTest extends FunSuite
    with JenaModule
    with RDFStoreLocalJena1Provider
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  import ops._
  import rdfStore.transactorSyntax._

  test("TDB contains label") {
    val uri = URI("http://jmvanel.free.fr/jmv.rdf#me")
    readStoreUriInNamedGraph(uri)
    val res = {
      rdfStore.rw( dataset, {
        val lab = instanceLabel(uri, allNamedGraph, "fr")
        println(s"label for $uri: $lab")
        // TODO check that the value is stored in TDB
      })
    }
    println(res)
  }

}