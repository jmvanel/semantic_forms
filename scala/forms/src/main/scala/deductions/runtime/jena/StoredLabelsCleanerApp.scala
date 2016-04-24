package deductions.runtime.jena

import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory

/** How to enumerate the languages used by all users? */
object StoredLabelsCleanerApp extends RDFStoreLocalJena1Provider
    with App
    with InstanceLabelsInferenceMemory[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  //  close()
  for (lang <- List("en", "fr", "")) {
    cleanStoredLabels(lang)
  }
  close()
}