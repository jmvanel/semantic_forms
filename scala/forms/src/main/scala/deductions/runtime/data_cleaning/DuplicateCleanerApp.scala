package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider

/** merges Duplicates in default database (TDB/ ) */
object DuplicateCleanerApp extends App
    with RDFStoreLocalJena1Provider
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  // override val databaseLocation: String = "" // in-memory
  override val deleteDatabaseLocation = true
  override val useTextQuery = false

  val classURIForMergingInstances = ops.URI(args(0))
  val lang = if (args.size > 1) args(1) else "en"
  removeAllDuplicates(classURIForMergingInstances, lang)
}