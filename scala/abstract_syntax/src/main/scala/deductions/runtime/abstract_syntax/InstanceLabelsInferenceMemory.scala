package deductions.runtime.abstract_syntax

import deductions.runtime.sparql_cache.dataset.DatasetHelper
import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import org.w3.banana.RDF

import scala.util.Success
import scalaz._
import Scalaz._
import scala.concurrent.Future
import scala.util.Try

/** wraps InstanceLabelsInference to cache Instance Labels in TDB
 *  should be called InstanceLabelsInferenceMemoization , cf https://en.wikipedia.org/wiki/Memoization TODO */
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
  lazy val labelsGraphUriPrefix = "urn:/semforms/labelsGraphUri/"
  lazy val displayLabelPred = URI("urn:displayLabel")

  /** make Instance Label, by retrieving from TDB, or else compute and Store In TDB
   *  NON transactional, needs rw transaction */
  override def makeInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    if (labelFromTDB === "" || labelFromTDB === "Thing" || isLabelLikeURI(node, labelFromTDB ) )
      computeInstanceLabelAndStoreInTDB(node, graph, lang)
    else labelFromTDB
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  /** get instance Label From TDB, and if recorded label is not usable, compute it in a Future
   *  NON transactional, needs r transaction */
  def makeInstanceLabelFuture(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDB = instanceLabelFromTDB(node, lang)
    launchLabelComputeInFuture(node, labelFromTDB, graph, lang)
  }

  /** get instance Label From TDB, and if recorded label is not usable, compute it in a Future
   * Transactional, creates a Read transaction */
  def makeInstanceLabelFutureTr(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val labelFromTDBTry =
      wrapInReadTransaction { instanceLabelFromTDB(node, lang) }
    val labelFromTDB = labelFromTDBTry.getOrElse("")
    launchLabelComputeInFuture(node, labelFromTDB, graph, lang)
  }

  private def launchLabelComputeInFuture(node: Rdf#Node, labelFromTDB: String,
      graph: Rdf#Graph, lang: String): String = {
     if (labelFromTDB === "" ||
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

  /** get instance (URI node) Label From TDB
   *  NON transactional, needs r transaction */
  def instanceLabelFromTDB(node: Rdf#Node, lang: String): String = {
    val localDebuginstanceLabelFromTDB = false
    def printlnLocal(s: String): Unit = if(localDebuginstanceLabelFromTDB) println(s)
    if( nodeToString(node) === "" ) return ""
	    printlnLocal(s"""\ninstanceLabelFromTDB node <$node> """ )
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    val labelsGraph0 = rdfStore.getGraph( datasetForLabels, labelsGraphUri)
	    // printlnLocal( s"instanceLabelFromTDB after .getGraph( dataset3, labelsGraphUri) $dataset3 ${Try{labelsGraph0}}" )
    val labelsGraph = labelsGraph0.get
	    // printlnLocal( s"instanceLabelFromTDB after labelsGraph.get '$labelsGraphUri' ${Try{labelsGraph}}")
      printlnLocal( s"instanceLabelFromTDB: labelsGraphUri $labelsGraphUri , labelsGraph $labelsGraph" )
    val displayLabelsIt = find(labelsGraph, node, displayLabelPred, ANY).toIterable
    val i18nGraph = rdfStore.getGraph( dataset, URI("urn:rdf-i18n")).getOrElse(emptyGraph)
    val displayLabelsIt2 = find( i18nGraph, node, rdfs.label, ANY).toIterable
	    printlnLocal(s"instanceLabelFromTDB after find(labelsGraph, node, displayLabelPred, ANY) : displayLabelPred $displayLabelPred '${displayLabelsIt.mkString("; ")}'" )
    (displayLabelsIt ++ displayLabelsIt2) match {
      case it if (!it.isEmpty) =>
         printlnLocal( s"recover displayLabel from TDB: <$node>" )
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

  /** compute Instance Label and store it in TDB,
   *  then replace label in special named Graph */
  def replaceInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    val label = computeInstanceLabelAndStoreInTDB(node, graph, lang)
    // Thread.sleep(30000) // test interrupted HTTP request
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    replaceObjects( labelsGraphUri, node, displayLabelPred, Literal(label), datasetForLabels )
    label
  }

  /** compute Instance Label and store it in TDB */
  private def computeInstanceLabelAndStoreInTDB(node: Rdf#Node, graph: Rdf#Graph, lang: String): String = {
    logger.debug(s"compute displayLabel for <$node>")
    if (node.toString() === "") return ""

    val labelFromNode = super.makeInstanceLabel(node, graph, lang)
    logger.debug(s"computeInstanceLabeAndStoreInTDB: $node , computed label '$labelFromNode'")
    val label2 =
      if (labelFromNode === "" || isLabelLikeURI(node: Rdf#Node, labelFromNode)) {
        val labelFromLabelProperty = instanceLabelFromLabelProperty(node)
        logger.debug(s"computeInstanceLabeAndStoreInTDB: $node labelFromLabelProperty $labelFromLabelProperty")
        labelFromLabelProperty match {
          case Some(node) =>
            foldNode(node)(
              uri => makeInstanceLabel(uri, graph, lang),
              bn => makeInstanceLabel(node, graph, lang),
              lit => fromLiteral(lit)._1)
          case _ => labelFromNode
        }
      } else labelFromNode
    storeInstanceLabel(node, label2, graph, lang)
    label2
  }

  def isLabelLikeURI(node: Rdf#Node, label: String): Boolean = {
    val nodeString = node.toString()
    nodeString.endsWith(label) ||
    nodeString.endsWith(label.substring(0, label.length()-1)) ||
    label.endsWith("#")
  }

  private def storeInstanceLabel(node: Rdf#Node, label: String,
                                 graph: Rdf#Graph, lang: String): Try[Unit] = {
    def doStore: Try[Unit] = {
      val computedLabelTriple = (node -- displayLabelPred ->- Literal(label)).graph
      val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
      rdfStore.appendToGraph(datasetForLabels, labelsGraphUri, computedLabelTriple)
    }
    if (label =/= "" && !isLabelLikeURI(node, label)) {
      logger.debug(s"storeInstanceLabel: actually store: node <$node>, label '$label', lang $lang")
      foldNode(node)(
        uri => doStore,
        bn => doStore,
        literal => {
          logger.error(s">>>> storeInstanceLabel(node=$node, label⁼$label): Node should be URI or BN")
          Success(Unit)
        })
    } else Success(Unit)
  }

  def cleanStoredLabels(lang: String) {
    val labelsGraphUri = URI(labelsGraphUriPrefix + lang)
    rdfStore.removeGraph( datasetForLabels, labelsGraphUri)
  }
}
