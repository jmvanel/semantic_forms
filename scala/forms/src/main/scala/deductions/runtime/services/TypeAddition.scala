package deductions.runtime.services

import scala.language.postfixOps

import org.w3.banana.FOAFPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFSPrefix

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.utils.URIHelpers
import deductions.runtime.utils.URIManagement

/**
 * ensure that types inferred from ontologies are added to objects of given triples
 *  USE CASE: when user has entered a new value V for an object property,
 *  associate an rdf:type to this value,
 *  so that the form for V will be correctly populated.
 */
trait TypeAddition[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
		with URIHelpers
		with URIManagement {

  import ops._

  private lazy val rdfs = RDFSPrefix[Rdf]
  private lazy val foaf = FOAFPrefix[Rdf]

  /** add Type triples, inferred from rdfs:range's
   *
   * NON transactional */
  def addTypes(triples: Seq[Rdf#Triple], graphURI: Option[Rdf#URI]) = {
    val graph = allNamedGraph
    val v = for (triple <- triples) yield addType(triple, graphURI, graph)
    v.flatten
  }

  /** @param triple ?S ?P ?O
   *
   * the type(s) ?C of ?O is inferred from rdfs:range of ?P,
   *  and then these triples are added in given graph URI:
   *  ?O a ?C
   *  NON transactional */
  def addType(triple: Rdf#Triple, graphURI: Option[Rdf#URI],
    graph: Rdf#Graph = allNamedGraph): Iterable[Rdf#Triple] = {
    val objectt = triple.objectt
    val pgObjectt = PointedGraph[Rdf](objectt, graph)

    /* these triples are added in given graph URI:  ?O a ?C */
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

    //// body of function addType()

    val result = if (objectt.isURI) {
      val pgObjectt = PointedGraph[Rdf](objectt, graph)
      val existingTypes = (pgObjectt / rdf.typ).nodes
      if (existingTypes isEmpty) {
        addRDFSLabelValue(objectt, graphURI) // PENDING move out of the if() block
        addTypeValue()
      } else Seq()
    } else Seq()
    result
  }

  /**
   * if there is not already some rdfs.label, foaf.lastName, foaf.familyName properties set,
   * add a triple
   * ?O rdfs.label ?LAB ,
   * where ?LAB is computed from URI string of ?O
   * NOTES:
   * - related to InstanceLabelsInference2#instanceLabel(), but here we actually add a triple,
   *     because we are in a callback for user edits
   * - used also for plain HTML page annotation
   */
  def addRDFSLabelValue(resource: Rdf#Node, graphURI: Option[Rdf#URI], graph: Rdf#Graph = allNamedGraph) = {
    val pgObjectt = PointedGraph[Rdf](resource, graph)

    val existingValues = (pgObjectt / rdfs.label).nodes
    val existingValues2 = (pgObjectt / foaf.lastName).nodes
    val existingValues3 = (pgObjectt / foaf.familyName).nodes
    if (existingValues.isEmpty &&
      existingValues2.isEmpty &&
      existingValues3.isEmpty &&
      (!isAbsoluteURI(resource.toString()) ||
        resource.toString().startsWith(instanceURIPrefix))) {
      if (isAbsoluteURI(resource.toString()))
        println("isAbsoluteURI " + resource)
      val labelTriple = makeTriple(
        resource, rdfs.label,
        Literal(makeStringFromURI(resource.toString())))
      rdfStore.appendToGraph(dataset, makeGraphForSaving(resource, graphURI), ops.makeGraph(Seq(labelTriple)))
    }
  }

  def makeGraphForSaving(objectt: Rdf#Node, graphURI: Option[Rdf#URI]) = {
      graphURI.getOrElse(
        foldNode(objectt)(
          u => u,
          bn => URI(""),
          lit => URI("")))
    }
}

