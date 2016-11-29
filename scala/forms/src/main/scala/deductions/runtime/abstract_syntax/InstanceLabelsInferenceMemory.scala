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
    with RDFHelpers[Rdf]
    with DatasetHelper[Rdf, DATASET]
    with InstanceLabelsFromLabelProperty[Rdf, DATASET] {

  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._
  private val logger: Logger = Logger.getRootLogger()

  val datasetForLabels = dataset
  /* TODO : does not work because the transaction has been started on the other dataset ! 
  val datasetForLabels = dataset3   */
  val labelsGraphUriPrefix = "urn:/semforms/labelsGraphUri/"
  val displayLabelPred = URI("urn:displayLabel")

  /** NON transactional, needs rw transaction */
  override def instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    if (labelFromTDB == "" || labelFromTDB == "Thing" || isLabelLikeURI(node, labelFromTDB ) )
      computeInstanceLabeAndStoreInTDB(node, graph, lang)
    else labelFromTDB
  }

  /** NON transactional, needs rw transaction */
  def instanceLabelFromTDB(node: Rdf#Node, lang: String): String = {
    if( node.toString() == "" ) return ""
//	  println(s"""instanceLabelFromTDB node "$node" """ )
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    val labelsGraph0 = rdfStore.getGraph( datasetForLabels, labelsGraphUri)
//	  println( s"instanceLabelFromTDB after .getGraph( dataset3, labelsGraphUri) $dataset3 $labelsGraph0" )
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

  /** compute Instance Label and store it in TDB,
   *  then replace label in special named Graph */
  def replaceInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val label = computeInstanceLabeAndStoreInTDB(node, graph, lang)

    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    replaceObjects( labelsGraphUri, node, displayLabelPred, Literal(label), datasetForLabels )
    label
  }

  /** compute Instance Label and store it in TDB */
  private def computeInstanceLabeAndStoreInTDB(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    logger.debug( s"compute displayLabel for $node" )
    if( node.toString() == "" ) return ""

    val label = super.instanceLabel(node, graph, lang)
    println(s"computeInstanceLabeAndStoreInTDB: $node .toString() , label $label")
    println(s"$node .toString().endsWith( label.substring(label.length()-1) = ${label.substring(0, label.length()-1)}")
    val label2 = if( label == "" || isLabelLikeURI(node: Rdf#Node, label) ) {
      val v = instanceLabelFromLabelProperty(node)
      v match {
        case Some(node) =>
          foldNode(node)( uri => instanceLabel(uri, graph, lang),
              _ => instanceLabel(node, graph, lang),
              lab => fromLiteral(lab)._1 )
        case _ => label
      }
  }  else label
    storeInstanceLabel(node, label2, graph, lang)
    label2
  }
  
  def isLabelLikeURI(node: Rdf#Node, label: String) =
		  node.toString().endsWith(label) ||
		  node.toString().endsWith(label.substring(0, label.length()-1))

  private def storeInstanceLabel(node: Rdf#Node, label: String,
                                 graph: Rdf#Graph, lang: String) = {
    if (label != "") {
      val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
      val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
      rdfStore.appendToGraph(datasetForLabels, labelsGraphUri, computedDisplayLabel)
    }
  }
  
  def cleanStoredLabels(lang: String) {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    rdfStore.removeGraph( datasetForLabels, labelsGraphUri)
  }
}
