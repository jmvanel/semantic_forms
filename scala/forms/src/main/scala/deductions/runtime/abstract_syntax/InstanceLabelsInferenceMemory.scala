package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.DatasetHelper
import org.apache.log4j.Logger

/** wraps InstanceLabelsInference to cache Instance Labels in TDB */
trait InstanceLabelsInferenceMemory[Rdf <: RDF, DATASET]
    extends InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with RDFCacheAlgo[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with DatasetHelper[Rdf, DATASET] {

  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._
  private val logger: Logger = Logger.getRootLogger()

  val dataset3 = dataset
  /* TODO : does not work because the transaction has been started on the other dataset ! 
  val dataset3 = createDatabase("TDBlabels")
   */
  val labelsGraphUriPrefix = "urn:/semforms/labelsGraphUri/"
  val displayLabelPred = URI("urn:displayLabel")

  /** NON transactional, needs rw transaction */
  override def instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    if (labelFromTDB == "")
      computeInstanceLabel(node, graph, lang)
    else labelFromTDB
  }

  def instanceLabelFromTDB(node: Rdf#Node, lang: String): String = {
//	  println(s"""instanceLabelFromTDB node "$node" """ )
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    val labelsGraph0 = dataset.getGraph(labelsGraphUri)
//	  println( s"instanceLabelFromTDB after dataset3.getGraph(labelsGraphUri) $dataset3 $labelsGraph0" )
    val labelsGraph = labelsGraph0.get
//	  println( s"instanceLabelFromTDB after labelsGraph.get $labelsGraphUri $labelsGraph")
    val displayLabelsIt = find(labelsGraph, node, displayLabelPred, ANY)
//	  println(s"instanceLabelFromTDB after find(labelsGraph, node, displayLabelPred, ANY)" )
    displayLabelsIt.toIterable match {
      case it if (!it.isEmpty) =>
        // println( s"recover displayLabel from TDB: $node" )
        val tr = it.head
//        println("instanceLabelFromTDB tr " + tr)
        val label = tr.objectt
        foldNode(label)(_ => "", _ => "", lit => fromLiteral(lit)._1)
      case _ => ""
    }
  }

  override def instanceLabels(list: Seq[Rdf#Node], lang: String = "")
  (implicit graph: Rdf#Graph): Seq[String] =
    list.map { node => instanceLabelFromTDB(node, lang)
  }
    
  def replaceInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    val label = computeInstanceLabel(node, graph, lang)
    replaceObjects( labelsGraphUri, node, displayLabelPred, Literal(label), dataset3 )
    label
  }

  /** compute Instance Label and store it in TDB */
  def computeInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    logger.debug( s"compute displayLabel for $node" )
    val label = super.instanceLabel(node, graph, lang)
    val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
    dataset3.appendToGraph(labelsGraphUri, computedDisplayLabel)
    label
  }

}
