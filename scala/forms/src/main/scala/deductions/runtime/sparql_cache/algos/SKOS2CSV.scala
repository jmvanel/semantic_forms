package deductions.runtime.sparql_cache.algos

import java.io.FileReader
import java.io.PrintStream

import scala.collection.immutable.ListMap

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFPrefix

import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import java.io.File
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFHelpers0

/** output given SKOS file as CSV */
object SKOS2CSVApp extends App
with JenaModule
with DefaultConfiguration
with SKOS2CSV[Jena]
{
  val addEmptyLineBetweenLabelGroups = false

  val inputFile = args(0)
  
  val graph = rdfLoader.load( new File(inputFile).toURI().toURL() ) . get

  val classToReportURI = owlClassToReport(args)
  val instancesURI = findInstances(graph, classToReportURI)

  val outputFile = inputFile + "." +
    // rfsLabel(classToReportURI, graph).replace(":","=") +
    terminalPart(classToReportURI) +
		".csv"
  override val printStream = new PrintStream(outputFile)

  val instancesgroupedByRdfsLabel0: Map[String, List[Rdf#Node]] =
    instancesURI.
    groupBy { n => rdfsLabel(n, graph) }
  val instancesgroupedByRdfsLabel = ListMap(instancesgroupedByRdfsLabel0.toSeq.
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
          case _ => ""
        }
        val digestFromClass = "\t" +
          (if (classToReportURI == owl.Class)
            makeDigestFromClass(n, graph)
          else "")
        s"\t'${labelAndList._1}'\t" + abbreviateURI(n) + "\t" + contextLabel + "\t" +
        rangeLabel + digestFromClass
      }
      columns.mkString("\n") + (
        if (addEmptyLineBetweenLabelGroups)
          "\n"
        else "")
    }
    // TODO I18N
    //      A       B       C   D         E                 F                     G
    output("Action	Libellé	Id	Contexte	type(rdfs:range)	Empreinte(propriétés)	Description")
    instancesgroupedByRdfsLabel.map {
      labelAndList => formatCSVLines(labelAndList)
    }.mkString("\n")
  }

  def formatIndentedText() = {
    instancesgroupedByRdfsLabel.map {
      labelAndList =>
        s"'${labelAndList._1}'\n" +
          (labelAndList._2).map { n => abbreviateURI(n) }.sorted.mkString("\t", "\n\t", "")
    }.mkString("\n")
  }
}

trait SKOS2CSV[Rdf <: RDF]
    extends DuplicatesDetectionBase[Rdf]
    with RDFHelpers[Rdf] {
  this: Configuration =>

  override lazy val rdf = RDFPrefix[Rdf]

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  implicit val ops: RDFOps[Rdf]

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
