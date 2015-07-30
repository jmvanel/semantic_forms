package deductions.runtime.services

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix
import org.w3.banana.SparqlGraphModule

import deductions.runtime.dataset.RDFStoreLocalProvider

trait TypeAddition[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with SparqlGraphModule {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  val rdfs = RDFSPrefix[Rdf]

  def addTypes(triples: Seq[Rdf#Triple], graphURI: Rdf#URI) = {
    for (triple <- triples) {
      addType(triple, graphURI)
    }
  }

  def addType(triple: Rdf#Triple, graphURI: Rdf#URI) = {
    val pg = PointedGraph[Rdf](triple.predicate, allNamedGraph)
    val cls = pg / rdfs.range
    val classes = cls.nodes
    val subject = triple.subject
    val typeTriples = for (classe <- classes)
      yield ops.makeTriple(subject, rdf.typ, classe)
    dataset.appendToGraph(graphURI, ops.makeGraph(typeTriples))
  }

}

