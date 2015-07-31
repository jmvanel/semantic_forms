package deductions.runtime.services

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix
import org.w3.banana.SparqlGraphModule
//import org.w3.banana.syntax._
import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * ensure that types inferred from ontologies are added to objects of given triples
 *  USE CASE: when user as entered a new value V for an object property,
 *  associate an rdf:type to this value,
 *  so that the form for V will be correctly populated.
 */
trait TypeAddition[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with SparqlGraphModule {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  private val rdfs = RDFSPrefix[Rdf]

  def addTypes(triples: Seq[Rdf#Triple], graphURI: Option[Rdf#URI]) = {
    val v = for (triple <- triples) yield addType(triple, graphURI)
    v.flatten
  }

  /** NON transactional */
  def addType(triple: Rdf#Triple, graphURI: Option[Rdf#URI]) = {
    val objectt = triple.objectt
    if (objectt.isURI) {
      val pgObjectt = PointedGraph[Rdf](objectt, allNamedGraph)
      val existingTypes = (pgObjectt / rdf.typ).nodes
      if (existingTypes isEmpty) {
        val pgPredicate = PointedGraph[Rdf](triple.predicate, allNamedGraph)
        val cls = pgPredicate / rdfs.range
        val classes = cls.nodes
        val typeTriples = for (classe <- classes)
          yield ops.makeTriple(objectt, rdf.typ, classe)
        val grURI = graphURI.getOrElse(
          foldNode(objectt)(
            u => u,
            bn => URI(""),
            lit => URI("")))
        dataset.appendToGraph(grURI, ops.makeGraph(typeTriples))
        typeTriples
      } else Seq()
    } else Seq()
  }

}

