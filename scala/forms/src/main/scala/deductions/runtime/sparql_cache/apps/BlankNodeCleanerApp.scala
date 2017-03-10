package deductions.runtime.sparql_cache.apps

import deductions.runtime.data_cleaning.BlankNodeCleanerBatch
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

object BlankNodeCleanerApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFCache
    with App
    with BlankNodeCleanerBatch[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  //  val config = new DefaultConfiguration {
  //    override val useTextQuery = false
  //  }
  cleanUnreachableBlankNodeSubGraph()
}