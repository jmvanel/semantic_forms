package deductions.runtime.abstract_syntax

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.DefaultConfiguration
import org.scalatest.FunSuite
import org.w3.banana.jena.JenaModule

class InstanceLabelsInferenceMemoryTest extends FunSuite
    with JenaModule
    with RDFStoreLocalJenaProvider
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  import ops._

  test("TDB contains label") {
    val uri = URI("http://jmvanel.free.fr/jmv.rdf#me")
    readStoreUriInNamedGraph(uri)
    val res = {
      rdfStore.rw( dataset, {
        val lab = makeInstanceLabel(uri, allNamedGraph, "fr")
        println(s"label for $uri: $lab")
        // TODO check that the value is stored in TDB
      })
    }
    println(res)
  }

}