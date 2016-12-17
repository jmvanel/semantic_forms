package deductions.runtime.jena

import deductions.runtime.data_cleaning.NamedGraphsCleaner
import deductions.runtime.services.DefaultConfiguration

object NamedGraphsCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with NamedGraphsCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  cleanDBPediaGraphs()
}