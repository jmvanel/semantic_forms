package deductions.runtime.utils

import org.w3.banana.{OWLPrefix, PointedGraph, Prefix, RDF, RDFOps, RDFPrefix}

trait RDFTreeDuplicator[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  def duplicateTree( root: Rdf#URI, newRoot: Rdf#URI, graph: Rdf#Graph ): 
  Iterator[Rdf#Triple] = {
    val triples = find(graph, root, ANY, ANY)
    for ( t <- triples ) yield
      makeTriple(newRoot, t.predicate, t.objectt)
  }
}