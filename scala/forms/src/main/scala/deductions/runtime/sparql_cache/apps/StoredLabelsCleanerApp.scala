package deductions.runtime.sparql_cache.apps

import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

/** How to enumerate the languages used by all users? */
object StoredLabelsCleanerApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFCache
    with App
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  //  close()
  for (lang <- List("en", "fr", "")) {
    cleanStoredLabels(lang)
  }
  close()
}