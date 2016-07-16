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
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration

/** Duplicates Detection for OWL; output: CSV, Grouped By labels of Datatype properties,
 *  or  owl:ObjectProperty", or "owl:Class"*/
object DuplicatesDetectionOWLGroupBy extends App with JenaModule with DuplicatesDetectionOWL[Jena]
with DefaultConfiguration {
  val addEmptyLineBetweenLabelGroups = false // true

  val owlFile = args(0)

  val graph = turtleReader.read(new FileReader(owlFile), "").get
  
  val classToReportURI = owlClassToReport(args)

  val datatypePropertiesURI = findInstances(graph, classToReportURI)

  val outputFile = owlFile + "." + rdfsLabel(classToReportURI, graph) + ".group_by_label.csv"
  override val printStream = new PrintStream(outputFile)

  val datatypePropertiesgroupedByRdfsLabel0 = datatypePropertiesURI.
    groupBy { n => rdfsLabel(n, graph) }
  val datatypePropertiesgroupedByRdfsLabel = ListMap(datatypePropertiesgroupedByRdfsLabel0.toSeq.
    sortBy(_._1): _*)
  val report = formatCSV()
  output(s"$report")
  outputErr(s"File written: $outputFile")

  /** format report as CSV */
  def formatCSV(): String = {
    /** format Label Group as CSV */
    def formatCSVLines(labelAndList: (String, List[Rdf#Node])) = {
      val list = labelAndList._2
      val columns = for (n <- list) yield {
        val rdfs_domain = rdfsDomain(n, graph)
        val domainLabel = rdfsLabel(rdfs_domain, graph)
        val superClassesLabel = rdfsSuperClasses(rdfs_domain, graph).
          map { superC => rdfsLabel(superC, graph) }.
          mkString(", ")
        val rdfs_range = rdfsRange(n, graph)
        val rangeLabel = rdfsLabel(rdfs_range.headOption, graph)
        val contextLabelProperty = domainLabel + (if (!superClassesLabel.isEmpty) " --> " + superClassesLabel else "")
        val contextLabel = classToReportURI match {
          case owl.ObjectProperty   => contextLabelProperty
          case owl.DatatypeProperty => contextLabelProperty
          case owl.Class            => rdfsPropertiesAndRangesFromClass(n,graph)
        }
        s"\t'${labelAndList._1}'\t" + abbreviateURI(n) + "\t" + contextLabel + "\t" + rangeLabel
      }
      columns.mkString("\n") + (
        if (addEmptyLineBetweenLabelGroups)
          "\n"
        else "")
    }
    // TODO I18N
    output("Action	Libellé	Id	Contexte	type(rdfs:range)	Description")
    datatypePropertiesgroupedByRdfsLabel.map {
      labelAndList => formatCSVLines(labelAndList)
    }.mkString("\n")
  }

  def formatIndentedText() = {
    datatypePropertiesgroupedByRdfsLabel.map {
      labelAndList =>
        s"'${labelAndList._1}'\n" +
          (labelAndList._2).map { n => abbreviateURI(n) }.sorted.mkString("\t", "\n\t", "")
    }.mkString("\n")
  }
}

/**
 * This App outputs too much : count n*(n-1)/2 ;
 *  rather use DuplicatesDetectionOWLGroupBy
 */
object DuplicatesDetectionOWLApp extends App with JenaModule with DuplicatesDetectionOWL[Jena]
with DefaultConfiguration {
  val owlFile = args(0)
  override val printStream = new PrintStream(owlFile + ".DuplicatesDetectionOWL.csv" )
  val graph = turtleReader.read(new FileReader(owlFile), "").get
  val duplicates = findDuplicateDataProperties(graph)
  output(s"duplicates size ${duplicates.duplicates.size}\n")

  val v = duplicates.duplicates.map { dup => dup toString (graph) }
  output(v.mkString("\n"))
  output(s"duplicates size ${duplicates.duplicates.size}")
}

trait DuplicatesDetectionOWL[Rdf <: RDF]
    extends DuplicatesDetectionBase[Rdf] {
    this: Configuration =>

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  implicit val ops: RDFOps[Rdf]
  import ops._

  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
    val datatypePropertiesURI = findInstances(graph)
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2).toList
    output(s"datatype Properties pairs count n*(n-1)/2 = ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs
      pairList: List[Rdf#Node] = pair.toList
      datatypeProperty1 :: datatypeProperty2 :: rest = pairList if (
        nodesAreSimilar(datatypeProperty1, datatypeProperty2, graph))
      //        _ = log(s"pair $pair")
    } yield Duplicate(datatypeProperty1, datatypeProperty2)

    DuplicationAnalysis(pairs.toList)
  }

}
