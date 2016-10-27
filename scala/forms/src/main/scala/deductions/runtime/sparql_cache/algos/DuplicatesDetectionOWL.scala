package deductions.runtime.sparql_cache.algos

import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix

import scala.collection.immutable.ListMap
import java.io.PrintStream
import java.io.FileInputStream

import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider

/** Duplicates Detection for OWL; output: CSV, Grouped By labels of Datatype properties,
 *  or  owl:ObjectProperty", or "owl:Class"
 *  
 *  deductions.runtime.sparql_cache.algos.DuplicatesDetectionOWLGroupBy
 *  */
object DuplicatesDetectionOWLGroupBy extends App
with RDFStoreLocalJena1Provider
with ImplementationSettings.RDFModule
with DefaultConfiguration
with DuplicatesDetectionOWL[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  import ops._

  val addEmptyLineBetweenLabelGroups = false // true
  val filterAmetysSubForms = false
  val propertiesToReport = List(URI("http://deductions.sf.net/ametys.ttl#category"), rdfs.subClassOf )
  
  val owlFile = args(0)

  val graph = turtleReader.read(new FileInputStream(owlFile), "").get
  
  val classToReportURI = owlMetaClassToReport(args)

  val instancesToReportURIs = {
    val allInstances = findInstances(graph, classToReportURI)
    // filter Ametys sub-forms
    if (filterAmetysSubForms)
      allInstances.filter { ins => !ins.toString().endsWith("#class") }
    else
      allInstances
  }

  val outputFile = owlFile + "." + rdfsLabel(classToReportURI, graph).replace(":","=") +
		  ".group_by_label.csv"
  override val printStream = new PrintStream(outputFile)

  val instancesToReportGroupedByRdfsLabel0: Map[String, List[Rdf#Node]] =
    instancesToReportURIs.
    groupBy { n => rdfsLabel(n, graph) }
  val instancesToReportGroupedByRdfsLabel = ListMap(instancesToReportGroupedByRdfsLabel0.toSeq.
    sortBy(_._1): _*)
  val report = formatCSV()
  output(s"$report")
  outputErr(s"File written: $outputFile")

  /** format report as CSV */
  def formatCSV(): String = {
    /** format Label Group as CSV */
    def formatCSVLines(labelAndList: (String, List[Rdf#Node])) = {
      val list = labelAndList._2
      val columns = for (
          node <- list
          if( ! node.isBlank() ) ) yield {

        val rdfs_domain = rdfsDomain(node, graph)
        val domainLabel = rdfsLabel(rdfs_domain, graph)
        val superClassesLabel = rdfsSuperClasses(rdfs_domain, graph).
          map { superC => rdfsLabel(superC, graph) }.
          mkString(", ")
        val rdfs_range = rdfsRange(node, graph)
        val rangeLabel = rdfsLabel(rdfs_range.headOption, graph)
        val contextLabelProperty = domainLabel + (if (!superClassesLabel.isEmpty) " --> " + superClassesLabel else "")
        val contextLabel = classToReportURI match {
          case owl.ObjectProperty   => contextLabelProperty
          case owl.DatatypeProperty => contextLabelProperty
          case owl.Class            => rdfsPropertiesAndRangesFromClass(node, graph)
        }
        val digestFromClass = "\t" +
          (if (classToReportURI == owl.Class)
            makeDigestFromClass(node, graph)
          else "")

        val otherProperties = propertiesToReport . map {
          p => printPropertyValueNoDefault(node, graph, p)
        } . mkString("\t")

        val id = abbreviateURI(node)
        val fullLine = s"\t'${labelAndList._1}'\t" + id + "\t" + contextLabel + "\t" +
        rangeLabel + digestFromClass + "\t" + otherProperties + "\n"

        fullLine + detailLines(node)
      }

      columns.mkString + (
        if (addEmptyLineBetweenLabelGroups)
          "\n"
        else "")
    }

    val headerEnd = propertiesToReport.map{
      p => "\t" + rdfsLabel(p, graph)
    } . mkString
    // TODO I18N
    //      A       B       C   D         E                 F                        G
    output("Action	Libellé	Id	Contexte	type(rdfs:range)	Empreinte(propriétés)" + headerEnd )
    instancesToReportGroupedByRdfsLabel.map {
      labelAndList => formatCSVLines(labelAndList)
    }. filter { c => c != "" } . mkString("\n")
  }

  /** detail Lines in between full Lines */
  def detailLines(n:  ImplementationSettings.Rdf#Node): String = {
    val firstColumns = "\t\t\t\t"
    rdfsPropertiesAndRangesFromClassList(n, graph) .
    mkString(firstColumns, "\n" + firstColumns, "")
  }
}


/**
 * This App outputs too much : count n*(n-1)/2 ;
 *  rather use DuplicatesDetectionOWLGroupBy
 */
object DuplicatesDetectionOWLApp extends App
with RDFStoreLocalJena1Provider
with ImplementationSettings.RDFModule
with DuplicatesDetectionOWL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
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

trait DuplicatesDetectionOWL[Rdf <: RDF, DATASET]
    extends DuplicatesDetectionBase[Rdf, DATASET]
{
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
