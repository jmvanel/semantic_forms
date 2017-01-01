package deductions.runtime.data_cleaning

import java.io.FileInputStream

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.algos.CSVFormatter
import deductions.runtime.services.DefaultConfiguration

object DuplicateCleanerSPARQLApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with DuplicateCleanerSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  //  val config = new DefaultConfiguration {
  //    override val useTextQuery = false
  //  }

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  implicit val graph = turtleReader.read(new FileInputStream(owlFile), "").get

  val homologURIGroups = groupBySPARQL(detectMergeableObjectProperties1, graph)

  for (homologURIGroup <- homologURIGroups) {
    for (homologURI <- homologURIGroup) {
    }
  }
}