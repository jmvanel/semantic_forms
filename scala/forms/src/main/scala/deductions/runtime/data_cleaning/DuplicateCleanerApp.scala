package deductions.runtime.data_cleaning

import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.utils.DefaultConfiguration

/**
 * merges Duplicates for given class URI
 * in default database (TDB/ )
 */
object DuplicateCleanerApp
    extends {
      override val config = new DefaultConfiguration {
        override val useTextQuery = false
      }
    } with App
    with RDFStoreLocalJenaProvider
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  // override val databaseLocation: String = "" // in-memory

  override val deleteDatabaseLocation = true
  //  val config = new DefaultConfiguration {
  //    override val useTextQuery = false
  //  }
  val classURIForMergingInstances = ops.URI(args(0))
  val lang = if (args.size > 1) args(1) else "en"
  removeAllDuplicates(classURIForMergingInstances, lang)
}