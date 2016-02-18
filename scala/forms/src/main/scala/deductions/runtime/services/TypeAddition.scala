package deductions.runtime.services

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix
import org.w3.banana.SparqlGraphModule
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.FOAFPrefix
import scala.language.postfixOps

/**
 * ensure that types inferred from ontologies are added to objects of given triples
 *  USE CASE: when user as entered a new value V for an object property,
 *  associate an rdf:type to this value,
 *  so that the form for V will be correctly populated.
 */
trait TypeAddition[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  private val rdfs = RDFSPrefix[Rdf]
  private val foaf = FOAFPrefix[Rdf]

  /** NON transactional */
  def addTypes(triples: Seq[Rdf#Triple], graphURI: Option[Rdf#URI]) = {
    val graph = allNamedGraph
    val v = for (triple <- triples) yield addType(triple, graphURI, graph)
    v.flatten
  }

  /** NON transactional */
  def addType(triple: Rdf#Triple, graphURI: Option[Rdf#URI],
    graph: Rdf#Graph = allNamedGraph): Iterable[Rdf#Triple] = {
    val objectt = triple.objectt
    val pgObjectt = PointedGraph[Rdf](objectt, graph)

    def addTypeValue() = {
      val existingTypes = (pgObjectt / rdf.typ).nodes
      if (existingTypes isEmpty) {
        val pgPredicate = PointedGraph[Rdf](triple.predicate, graph)
        val cls = pgPredicate / rdfs.range
        val classes = cls.nodes
        val typeTriples = for (classe <- classes)
          yield makeTriple(objectt, rdf.typ, classe)
        val grURI = graphURI.getOrElse(
          foldNode(objectt)(
            u => u,
            bn => URI(""),
            lit => URI("")))
        rdfStore.appendToGraph( dataset, grURI, ops.makeGraph(typeTriples))
        typeTriples
      } else Seq()
    }

    def addRDFSLabelValue() = {
      val existingValues = (pgObjectt / rdfs.label).nodes
      val existingValues2 = (pgObjectt / foaf.lastName).nodes
      val existingValues3 = (pgObjectt / foaf.familyName).nodes
      if (existingValues.isEmpty &&
        existingValues2.isEmpty &&
        existingValues3.isEmpty &&
        !objectt.toString().contains(":")) {
        val labelTriple = makeTriple(objectt, rdfs.label, Literal(objectt.toString().replace("_", " ")))
        rdfStore.appendToGraph( dataset, makeGraphForSaving(), ops.makeGraph(Seq(labelTriple)))
      }
    }

    def makeGraphForSaving() = {
      graphURI.getOrElse(
        foldNode(objectt)(
          u => u,
          bn => URI(""),
          lit => URI("")))
    }

    val result = if (objectt.isURI) {
      val pgObjectt = PointedGraph[Rdf](objectt, graph)
      val existingTypes = (pgObjectt / rdf.typ).nodes
      if (existingTypes isEmpty) {
        addRDFSLabelValue()
        addTypeValue()
      } else Seq()
    } else Seq()
    result
  }
}

