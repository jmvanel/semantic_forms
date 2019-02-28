package deductions.runtime.abstract_syntax

import deductions.runtime.sparql_cache.dataset.DatasetHelper
import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import org.w3.banana.RDF

import scala.util.Success
import scalaz._
import Scalaz._
import scala.concurrent.Future
import scala.util.Try

/** wraps InstanceLabelsInference to cache Instance Labels in TDB */
trait InstanceLabelsInferenceMemory[Rdf <: RDF, DATASET]
    extends InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with RDFHelpers[Rdf]
    with DatasetHelper[Rdf, DATASET]
    with InstanceLabelsFromLabelProperty[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  import ops._

  lazy val datasetForLabels = dataset
  /* TODO : does not work because the transaction has been started on the other dataset ! 
  val datasetForLabels = dataset3   */
  val labelsGraphUriPrefix = "urn:/semforms/labelsGraphUri/"
  val displayLabelPred = URI("urn:displayLabel")

  /** NON transactional, needs rw transaction */
  override def makeInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    if (labelFromTDB === "" || labelFromTDB === "Thing" || isLabelLikeURI(node, labelFromTDB ) )
      computeInstanceLabelAndStoreInTDB(node, graph, lang)
    else labelFromTDB
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  /** get instance Label From TDB, and if recorded label is not usable, compute it in a Future
   *  Transactional, creates a Read transaction */
  def makeInstanceLabelFuture(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    if (labelFromTDB === "" ||
        // labelFromTDB === "Thing" ||
        isLabelLikeURI(node, labelFromTDB))
      Future {
        wrapInTransaction(
          computeInstanceLabelAndStoreInTDB(node, graph, lang))
      }
    if (labelFromTDB.length() === 0)
      last_segment(node)
    else labelFromTDB
  }

  def instanceDescription(subjectNode: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    stringFromLiteralPred(subjectNode, rdfs.comment, lang)
      .getOrElse(stringFromLiteralPred(subjectNode, dct("description"), lang).getOrElse(""))
  }

  def instanceImage(subjectNode: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
//	  stringFromLiteralPred(subjectNode, foaf.img, lang ).getOrElse("")
	  stringFromObjectPred(subjectNode, foaf.img ).getOrElse("")
  }

  def instanceTypeLabel(subjectNode: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
	  stringFromObjectPred(subjectNode, rdf.typ ).getOrElse("")
  }

    def instanceRefCount(subjectNode: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
	  stringFromLiteralPred(subjectNode, form("linksCount") ).getOrElse("")
  }
    // http://raw.githubusercontent.com/jmvanel/semantic_forms/master/vocabulary/forms.owl.ttl#linksCount
  private def stringFromObjectPred(subjectNode: Rdf#Node, predNode: Rdf#Node): Option[String] = {
    for (
      triple <- find(allNamedGraph, subjectNode, predNode, ANY).toSeq.headOption;
      result = foldNode(triple.objectt)(
        uri => makeInstanceLabel(uri, allNamedGraph, "fr"),
        bn => makeInstanceLabel(bn, allNamedGraph, "fr"),
        literal => "")
    ) yield result
  }

  /** by Literal Predicate we mean a Predicate whose range is Literal */
  private def stringFromLiteralPred(
    subjectNode: Rdf#Node, predNode: Rdf#Node,
    lang: String = "en"): Option[String] = {
    subjectNode.fold(
      uri => {
        val triples = find(allNamedGraph, subjectNode, predNode, ANY).toIterable
        val values = triples.map(triple => triple.objectt)
        Some(
          getPreferedLanguageLiteral(values)(allNamedGraph, lang))
      },
      funBNode => None,
      funLiteral => None)
  }
//    for (
//      triple <- find(allNamedGraph, subjectNode, predNode, ANY).toSeq.headOption;
//      result = foldNode(triple.objectt)(
//        _ => "",
//        _ => "",
//        literal => fromLiteral(literal)._1)
//    ) yield result

  /** get instance (URI node) Label From TDB
   *  NON transactional, needs r transaction */
  def instanceLabelFromTDB(node: Rdf#Node, lang: String): String = {
    val localDebuginstanceLabelFromTDB = false
    def printlnLocal(s: String): Unit = if(localDebuginstanceLabelFromTDB) println(s)
    if( nodeToString(node) === "" ) return ""
	  printlnLocal(s"""\ninstanceLabelFromTDB node "$node" """ )
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    val labelsGraph0 = rdfStore.getGraph( datasetForLabels, labelsGraphUri)
	  // printlnLocal( s"instanceLabelFromTDB after .getGraph( dataset3, labelsGraphUri) $dataset3 ${Try{labelsGraph0}}" )
    val labelsGraph = labelsGraph0.get
	  // printlnLocal( s"instanceLabelFromTDB after labelsGraph.get $labelsGraphUri $${Try{labelsGraph}}")
    val displayLabelsIt = find(labelsGraph, node, displayLabelPred, ANY)
	  printlnLocal(s"instanceLabelFromTDB after find(labelsGraph, node, displayLabelPred, ANY)" )
    displayLabelsIt.toIterable match {
      case it if (!it.isEmpty) =>
         printlnLocal( s"recover displayLabel from TDB: $node" )
        val triple = it.head
        printlnLocal("instanceLabelFromTDB triple " + triple)
        val label = triple.objectt
        foldNode(label)(_ => "", _ => "", lit => fromLiteral(lit)._1)
      case _ => ""
    }
  }

  /** NON transactional */
  private def labelForURI(uri: String, language: String)
  (implicit graph: Rdf#Graph)
    : String = {
      makeInstanceLabel(URI(uri), graph, language)
  }

  override def instanceLabels(list: Seq[Rdf#Node], lang: String = "")
  (implicit graph: Rdf#Graph): Seq[String] =
    list.toList map { node => makeInstanceLabel(node, graph, lang) }
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
          val recompute = node_label._2 === ""
          (
            (if (recompute) {
              recomputeCount = recomputeCount + 1
              super.makeInstanceLabel(node_label._1, graph, lang)
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
                else Success(Unit)
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
    val label = computeInstanceLabelAndStoreInTDB(node, graph, lang)

    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    replaceObjects( labelsGraphUri, node, displayLabelPred, Literal(label), datasetForLabels )
    label
  }

  /** compute Instance Label and store it in TDB */
  private def computeInstanceLabelAndStoreInTDB(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    logger.debug( s"compute displayLabel for <$node>" )
    if( node.toString() === "" ) return ""

    val label = super.makeInstanceLabel(node, graph, lang)
//    println(s"computeInstanceLabeAndStoreInTDB: $node .toString() , computed label $label")
//    println(s"$node .toString().endsWith( label.substring(label.length()-1) = ${label.substring(0, label.length()-1)}")
    val label2 = if( label === "" || isLabelLikeURI(node: Rdf#Node, label) ) {
      val v = instanceLabelFromLabelProperty(node)
      v match {
        case Some(node) =>
          foldNode(node)(
              uri => makeInstanceLabel(uri, graph, lang),
              _ => makeInstanceLabel(node, graph, lang),
              lit => fromLiteral(lit)._1 )
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
    if (label  =/=  "") {
      if(!isURI(node))
        logger.error(s">>>> storeInstanceLabel(node=$node, label⁼$label): Node should be URI")
      val computedDisplayLabel = (node -- displayLabelPred ->- Literal(label)).graph
      val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
      rdfStore.appendToGraph(datasetForLabels, labelsGraphUri, computedDisplayLabel)
    } else Success(Unit)
  }
  
  def cleanStoredLabels(lang: String) {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    rdfStore.removeGraph( datasetForLabels, labelsGraphUri)
  }
}
