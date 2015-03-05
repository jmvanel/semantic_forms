package deductions.sparql

import org.w3.banana.RDFOpsModule

trait TestFixtureRDF extends RDFOpsModule {
  import ops._

  private val triples = List(makeTriple(
    makeUri("#i"),
    makeUri("knows"),
    makeUri("#me")))
  val graph = makeGraph(triples)
}