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
    if (labelFromTDB == "" || labelFromTDB == "Thing")
      computeInstanceLabel(node, graph, lang)
    else labelFromTDB
  }

  /** NON transactional, needs rw transaction */
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
    list.toList map { node => instanceLabel(node, graph, lang) }
    // list.map { node => instanceLabelFromTDB(node, lang)

  /** this was tried during trials to fix a ConcurrentModificationException,
   *  but the solution was in calling levels:
   *  https://github.com/jmvanel/semantic_forms/commit/6d1263530c337d69358670674de5178fd23d1765
   *   */
  private def instanceLabelsComplex(list: Seq[Rdf#Node], lang: String = "")
  (implicit graph: Rdf#Graph): Seq[String] = {
    val labelsFromTDB:Map[Rdf#Node, String] =
      list.toList.map { node => node -> instanceLabelFromTDB(node, lang) } . toMap
    var recomputeCount = 0
    val labelsComputedOrFromTDB =
      labelsFromTDB . map { (node_label)  =>
        node_label._1 -> {
          val recompute = node_label._2 == ""
          (
            (if (recompute) {
              recomputeCount = recomputeCount + 1
              super.instanceLabel(node_label._1, graph, lang)
            } else node_label._2),
            recompute)
        }
      }
      println("labelsComputedOrFromTDB size " + list.size + ", recomputeCount "+recomputeCount)
      labelsComputedOrFromTDB . map {
        node_label_boolean  =>
          if( node_label_boolean._2._2)
            storeInstanceLabel(node_label_boolean._1,
                node_label_boolean._2._1, graph, lang)
      }
      println("labelsComputedOrFromTDB 2")
      val ret = labelsComputedOrFromTDB . map {
        node_label_boolean  => node_label_boolean._2._1
      } . toList
      println("labelsComputedOrFromTDB 3")
      println("labelsComputedOrFromTDB 3 " + ret )
      ret
  }
    
  def replaceInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val label = computeInstanceLabel(node, graph, lang)
    
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    replaceObjects( labelsGraphUri, node, displayLabelPred, Literal(label), dataset3 )
    label
  }

  /** compute Instance Label and store it in TDB */
  private def computeInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    logger.debug( s"compute displayLabel for $node" )
    val label = super.instanceLabel(node, graph, lang)

//    val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
//    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
//    dataset3.appendToGraph(labelsGraphUri, computedDisplayLabel)
    storeInstanceLabel(node, label, graph, lang)
    label
  }
  
  private def storeInstanceLabel(node: Rdf#Node, label: String,
      graph: Rdf#Graph, lang: String) = {
    val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    dataset3.appendToGraph(labelsGraphUri, computedDisplayLabel)
  }
}
