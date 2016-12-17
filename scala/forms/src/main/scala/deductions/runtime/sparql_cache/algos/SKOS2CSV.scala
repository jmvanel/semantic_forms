package deductions.runtime.sparql_cache.algos

import java.io.File
import java.io.PrintStream

import scala.collection.immutable.ListMap

import org.w3.banana.OWLPrefix
import org.w3.banana.Prefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix

import deductions.runtime.abstract_syntax.FieldsInference
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.RDFHelpers

/** output given SKOS file as CSV */
object SKOS2CSVApp extends App
with RDFStoreLocalJena1Provider
with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with DefaultConfiguration 
with SKOS2CSV[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }

  val addEmptyLineBetweenLabelGroups = false
  override lazy val owl = OWLPrefix[Rdf]

  val inputFile = args(0)
  val graph = rdfLoader.load( new File(inputFile).toURI().toURL() ) . get

  val classToReportURI = owlMetaClassToReport(args)
  val instancesURI = findInstances(graph, classToReportURI)
  
  import ops._
  val ontologyURI = withoutFragment(classToReportURI)
  lazy val fieldsFromOntology = {
    //  val g1 = rdfLoader.load( new URL(fromUri(ontologyURI) ))
    // outputErr(s"class To Report ontology size ${g1.get.size}" )
	  val ontologyGraph = retrieveURI( ontologyURI) . get
	  rdfStore.r(dataset, fieldsFromClass(classToReportURI, ontologyGraph) . propertiesList . toList ) . get
  }
  val fields = {
    val skos = Prefix[Rdf]("skos", "http://www.w3.org/2004/02/skos/core#")
    List( skos("prefLabel"), skos("altLabel"), skos("broader"), skos("related") )
  }
  outputErr(s"fields ${fields.mkString(", ")}")
  
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

  val csvHeader =
    //A       B       C   D         E                 F                     G
    "Action	Libellé	Id	Contexte	type(rdfs:range)	Empreinte(propriétés)	Description"

  /** format report as CSV */
  def formatCSV(): String = {
    /** format Label Group as CSV */
    def formatCSVLines(labelAndList: (String, List[Rdf#Node])) = {
      val list = labelAndList._2
//      println(s"list size ${list.size}")
      val columns = for (n <- list) yield {
    	  val propsFromSubject0 = // fieldsFromSubject(n, graph) . toList
    	  fields
    	  val propsFromSubject = propsFromSubject0 . map { p => foldNode(p)(u=>u, _=>URI(""), _=>URI("")) }

    	  val r = propsFromSubject . map { p =>
    	    val objects = ops.getObjects(graph, n, p). map { o => o.toString() }
    	    objects . mkString("\t")
    	    }
    	  r  . mkString(",")
      }
      columns.mkString("\n") + (
        if (addEmptyLineBetweenLabelGroups)
          "\n"
        else "")
    }
    // TODO I18N
    output(csvHeader)
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

trait SKOS2CSV[Rdf <: RDF, DATASET]
    extends DuplicatesDetectionBase[Rdf, DATASET]
    with RDFHelpers[Rdf]
with FieldsInference[Rdf, DATASET]
{
  this: Configuration =>

  override lazy val rdf = RDFPrefix[Rdf]

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  implicit val ops: RDFOps[Rdf]

//  /** find fields from given Instance subject */
//  def fieldsFromSubject(subject: Rdf#Node, graph: Rdf#Graph): Seq[Rdf#URI] =
//    getPredicates(graph, subject).toSeq.distinct
    
}
