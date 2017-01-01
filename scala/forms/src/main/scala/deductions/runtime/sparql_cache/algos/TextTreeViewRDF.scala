package deductions.runtime.sparql_cache.algos

import java.io.FileInputStream
import java.io.PrintStream

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

/**
 * Duplicates Detection for OWL; output: CSV, Grouped By labels of Datatype properties,
 *  or  owl:ObjectProperty", or "owl:Class"
 */
object TextTreeViewRDF extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with DefaultConfiguration
    with DuplicatesDetectionBase[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  val graph = turtleReader.read(new FileInputStream(owlFile), "").get
  val owlClassToReportURI = owlMetaClassToReport(args)
  val instancesURI = findInstances(graph, owlClassToReportURI)

  val outputFile = owlFile + "." + rdfsLabel(owlClassToReportURI, graph) +
    ".TreeView.txt"
  override val printStream = new PrintStream(outputFile)
  val report = formatTreeView(instancesURI)
  output(s"$report")
  outputErr(s"File written: $outputFile")

  import ops._

  /** format tree view with tabs */
  def formatTreeView(instancesURI: List[Rdf#Node]) = {
    val roots = findRoots(instancesURI)
    val roots2 = findUndeclaredRoots(instancesURI)

    val undeclaredRoots = roots2.toSet.diff(roots.toSet)
    println(s"declared Roots: ${roots.size}")
    println(s"Undeclared Roots: ${undeclaredRoots.size} : $undeclaredRoots")
    val mergedRoots = (roots ::: roots2).toSet.toList
    val sortedMergedRoots = sortWithRdfsLabel(mergedRoots)
    println("sorted Merged Roots: " + sortedMergedRoots.size)
    sortedMergedRoots.foreach { indentURILabel(_, 0) }
  }

  /** TODO parameterize % predicate to build tree: currently hard-coded rdfs:subClassOf */
  def findRoots(instancesURI: List[Rdf#Node]): List[Rdf#Node] = {
    for (
      instance <- instancesURI;
      l = rdfsSuperClasses(instance, graph) if (l.isEmpty)
    ) yield instance
  }

  /**
   * find inheritance Roots ?R that are not declared by
   *  ?R a owl:Class
   */
  def findUndeclaredRoots(instancesURI: List[Rdf#Node]): List[Rdf#Node] = {
    val tr = find(graph, ANY, rdfs.subClassOf, ANY).map { tr => tr.objectt }.toList
    tr.toSet.toList
  }

  /** recursive */
  def indentURILabel(node: Rdf#Node, depth: Int): Unit = {
    val formattedURILabel = ("\t" * depth) + rdfsLabel(node, graph) + " - " + abbreviateURI(node)
    output(formattedURILabel)
    // recursive call 
    val subClasses = rdfsSubClasses(node, graph)
    sortWithRdfsLabel(subClasses).foreach { indentURILabel(_, depth + 1) }
  }

  def sortWithRdfsLabel(list: List[Rdf#Node]) = list.sortWith(
    (n1, n2) => rdfsLabel(n1, graph) < rdfsLabel(n2, graph))
}
