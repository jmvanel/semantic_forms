package deductions.runtime.sparql_cache.algos

import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import scala.collection.immutable.ListMap
import java.io.PrintStream

/**
 * Duplicates Detection for OWL; output: CSV, Grouped By labels of Datatype properties,
 *  or  owl:ObjectProperty", or "owl:Class"
 */
object TextTreeViewRDF extends App with JenaModule with DuplicatesDetectionBase[Jena] {

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  val graph = turtleReader.read(new FileReader(owlFile), "").get
  val owlClassToReportURI = owlClassToReport(args)
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
    println(s"Undeclared Roots: ${undeclaredRoots.size} : $undeclaredRoots")
    val sortedMergedRoots = sortWithRdfsLabel((roots ::: roots2).toSet.toList)
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

  /** find inheritance Roots ?R that are not declared by 
   *  ?R a owl:Class */
  def findUndeclaredRoots(instancesURI: List[Rdf#Node]): List[Rdf#Node] = {
	  val tr = find(graph, ANY, rdfs.subClassOf, ANY ) . map { tr => tr.objectt } . toList
    tr . toSet . toList
  }

  /** recursive */
  def indentURILabel(node: Rdf#Node, depth: Int): Unit = {
    val formattedURILabel = ("\t" * depth) + rdfsLabel(node, graph) +" - " + abbreviateURI( node )
    output( formattedURILabel )
    // recursive call 
    val subClasses = rdfsSubClasses( node, graph)
    sortWithRdfsLabel(subClasses).foreach { indentURILabel( _, depth+1) }
  }

  def sortWithRdfsLabel(list: List[Rdf#Node]) = list.sortWith(
      (n1, n2) => rdfsLabel(n1, graph) < rdfsLabel(n2, graph))
}
