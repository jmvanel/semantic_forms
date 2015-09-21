package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo

/** wraps InstanceLabelsInference to cache Instance Labels in TDB */
trait InstanceLabelsInferenceMemory[Rdf <: RDF, DATASET]
    extends InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with RDFCacheAlgo[Rdf, DATASET] {

  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

  val labelsGraphUriPrefix = "urn:/semforms/labelsGraphUri/"
  val displayLabelPred = URI("displayLabel")

  override def instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
      val labelsGraph = dataset.getGraph(labelsGraphUri).get
      val displayLabelsIt = find(labelsGraph, node, displayLabelPred, ANY)
      displayLabelsIt.toIterable match {
        case it if (!it.isEmpty) =>
          // recover displayLabel from TDB
//          println( s"recover displayLabel from TDB: $node" )
          val tr = it.head
          val label = tr.objectt
          foldNode(label)(_ => "", _ => "", lit => fromLiteral(lit)._1)
        case _ =>
          // compute displayLabel
          println( s"compute displayLabel for $node" )
          val label = super.instanceLabel(node, graph, lang)
          val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
          dataset.appendToGraph(labelsGraphUri, computedDisplayLabel)
          label
      }
  }
}