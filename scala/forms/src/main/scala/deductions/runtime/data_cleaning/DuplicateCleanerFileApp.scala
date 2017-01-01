package deductions.runtime.data_cleaning

import java.io.File

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.sparql_cache.RDFCacheAlgo

/**
 * merges Duplicates in given file(s),
 * among instances of given class URI
 *
 * Arguments
 * - class URI for instances
 * - files
 *
 * Output:
 * modified data file in /tmp (same name as input)
 */
object DuplicateCleanerFileApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  //  //  override val databaseLocation: String = "" // in-memory
  //  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  //  override val deleteDatabaseLocation = true
  //  override val useTextQuery = false

  println(s"databaseLocation $databaseLocation")
  duplicateCleanerFileApp()

  def duplicateCleanerFileApp() = {
    possiblyDeleteDatabaseLocation()
    val args2 = args.map { new File(_).getCanonicalPath }
    val classURI = ops.URI(args(0))
    println(s"classURI $classURI")
    val files = loadFilesFromArgs(args2)
    val lang = java.util.Locale.getDefault().getCountry
    println(removeAllDuplicates(classURI, lang))
    outputModifiedTurtle(files(0))
  }

}