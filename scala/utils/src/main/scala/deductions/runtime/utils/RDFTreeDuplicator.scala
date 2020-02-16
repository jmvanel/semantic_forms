package deductions.runtime.utils

import org.w3.banana.{OWLPrefix, PointedGraph, Prefix, RDF, RDFOps, RDFPrefix}

trait RDFTreeDuplicator[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  /** duplicate RDF Tree; needs transaction (because of find() */
  def duplicateTree( root: Rdf#URI, newRoot: Rdf#Node, graph: Rdf#Graph ):
  Iterator[Rdf#Triple] = {
    val triples = find(graph, root, ANY, ANY)
    for ( t <- triples ) yield
      makeTriple(newRoot, t.predicate, t.objectt)
  }
}