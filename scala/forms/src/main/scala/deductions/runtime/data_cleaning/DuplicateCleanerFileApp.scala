package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import java.io.File
import deductions.runtime.services.SPARQLHelpers
import java.io.FileWriter

/**
 * merges Duplicates in given file(s),
 * among instances of given class URI
 *
 * Arguments
 * - class URI for instances
 * - files
 */
object DuplicateCleanerFileApp extends App
    with RDFStoreLocalJena1Provider
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  override val databaseLocation: String = "" // in-memory
  // override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory

  duplicateCleanerFileApp

  def duplicateCleanerFileApp() = {
    val classURI = ops.URI(args(0))
    println(s"classURI $classURI")
    val files = args.slice(1, args.size)
    println(s"Files ${files.mkString(", ")}")

    for (file <- files) {
      println(s"Load file $file")
      retrieveURI(URI(new File(file).toURI().toASCIIString()))
    }
    // TODO get locale
    val lang = "fr"
    removeAllDuplicates(classURI, lang)

    outputModifiedTurtle(files)
  }

  /** output modified data in /tmp */
  def outputModifiedTurtle(files: Array[String]) = {
    val queryString = """
    CONSTRUCT { ?S ?P ?O }
    WHERE {
      GRAPH ?GR { ?S ?P ?O }
    } """
    val ttl = sparqlConstructQueryTR(queryString)
    val outputFile = "/tmp/" + new File(files(0)).getName
    println(s"Writing ${ttl.length()} chars in  output File $outputFile")
    val fw = new FileWriter(new File(outputFile))
    fw.write(ttl)
    fw.close()
  }
}