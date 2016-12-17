package deductions.runtime.data_cleaning

import java.io.FileInputStream

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.algos.CSVFormatter
import deductions.runtime.services.DefaultConfiguration

object DuplicateDetectionSPARQLApp extends App
    with ImplementationSettings.RDFCache
    with DuplicateCleanerSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSVFormatter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  val config = new DefaultConfiguration {
    // NOTE Lucene could be used !
    override val useTextQuery = false
  }

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  implicit val graph = turtleReader.read(new FileInputStream(owlFile), "").get

  val homologURIGroups = groupBySPARQL(detectMergeableObjectProperties1, graph)

  for (homologURIGroup <- homologURIGroups) {
    output(s"Groupe ${homologURIGroup.map { uri => rdfsLabel(uri, graph) }.mkString(", ")}")
    for (homologURI <- homologURIGroup) {
      val l = formatCSVLinesForNode(homologURI, owl.ObjectProperty, List(rdfs.domain))
      output(l)
    }
    output("\n\n")
  }
}