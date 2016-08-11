package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import java.io.File
import deductions.runtime.services.SPARQLHelpers
import java.io.FileWriter
import deductions.runtime.utils.CSVImporter
import java.io.FileInputStream

/**
 * merges Duplicates in given file(s),
 * among given instances;
 * NOTEs:
 * - no need to specify the class URI of merged instances,
 * since the instance URI's themselves are given,
 * - the columns names specifying the replaced URI & the replacing URI
 *   are controlled by the mappings in columnsMappings in trait [[deductions.runtime.utils.CSVImporter]]
 *
 * Arguments
 * - CSV Specification of pairs of URI: Duplicate and
 * - RDF files to load
 *
 * Output:
 * modified data file in /tmp (same name as input)
 */
object DuplicateCleanerSpecificationApp extends App
    with RDFStoreLocalJena1Provider
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  println(s"databaseLocation $databaseLocation")

  duplicateCleanerApp()

  def duplicateCleanerApp() = {
    loadFilesFromArgs(args)
    val csvSpecification = args(0)
    val propertyChanges = readCSVFile(csvSpecification)
    println(s"DuplicateCleanerSpecificationApp: propertyChanges: ${propertyChanges.size}")

    val v = propertyChanges.groupBy(pair => pair._2)
    val uriTokeep_duplicateURIs = v.map { case (uri, list) => (uri, list.map { el => el._1 }) }
    uriTokeep_duplicateURIs.map {
      case (uriTokeep, duplicateURIs) =>
        removeDuplicates(uriTokeep, duplicateURIs)
    }
    outputModifiedTurtle(csvSpecification + ".ttl")
  }

  /** read CSV file with columns restruc:property & restruc:replacingProperty */
  private def readCSVFile(file: String): List[(Rdf#URI, Rdf#URI)] = {
    val graph = run(new FileInputStream(file), URI("urn:/owl/transform"), List())
    println(s"DuplicateCleanerSpecificationApp: file $file: graph size: ${graph.size}")
    val queryString = """
         | PREFIX restruc: <http://deductions.github.io/restruc.owl.ttl#>
         | SELECT ?P ?RP
         | WHERE {
         | ?S restruc:replacingProperty ?RP ;
         |    restruc:property ?P .
         | }""".stripMargin
    val variables = Seq("P", "RP")
    val res = runSparqlSelect(queryString, variables, graph: Rdf#Graph)
    res.map { s => (s(0), s(1)) }
  }
}