package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider

object DuplicateCleanerApp extends App
    with RDFStoreLocalJena1Provider
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  val classURI = ops.URI(args(0))
  val lang = if (args.size > 1) args(1) else "en"
  removeAllDuplicates(classURI, lang)
}