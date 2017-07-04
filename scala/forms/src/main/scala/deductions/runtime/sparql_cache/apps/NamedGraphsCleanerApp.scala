package deductions.runtime.sparql_cache.apps

import deductions.runtime.data_cleaning.NamedGraphsCleaner
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration

object NamedGraphsCleanerApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFCache
    with App
    with NamedGraphsCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  cleanDBPediaGraphs()
}