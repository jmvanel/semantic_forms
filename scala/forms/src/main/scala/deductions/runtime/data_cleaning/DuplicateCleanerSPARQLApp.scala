package deductions.runtime.data_cleaning

import java.io.FileInputStream

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.algos.CSVFormatter

object DuplicateCleanerSPARQLApp extends App
    with ImplementationSettings.RDFCache
    with DuplicateCleanerSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

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