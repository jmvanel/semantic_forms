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
 * - CSV Specification of pairs of URI: Duplicate and kept URI
 * - RDF files to load
 *
 * Output:
 * modified data file in /tmp (same name as input)
 *
 * Properties used in CSV Specification: see "ONISEP" in [[CSVImporter.columnsMappings]]
 */
object DuplicateCleanerSpecificationApp extends App
    with RDFStoreLocalJena1Provider
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  override val deleteDatabaseLocation = true
  override val useTextQuery = false

  println(s"databaseLocation $databaseLocation")

  duplicateCleanerSpecificationApp()

  def duplicateCleanerSpecificationApp() = {
    possiblyDeleteDatabaseLocation()

    val args2 = args.map { new File(_).getCanonicalPath }

    loadFilesFromArgs(args2)
    val r = rdfStore.rw(dataset, {
      originalGraph = union(Seq(allNamedGraph))
      println(s"originalGraph size ${originalGraph.size()}")
    })

    val csvSpecification = args2(0)
    val propertyChanges = readCSVFile(csvSpecification)
    println(s"""DuplicateCleanerSpecificationApp:
      CSV input $csvSpecification,
      Change Specifications: size ${propertyChanges.size}""")

    val auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()

    val propertyChangesGroupedByReplacingURI =
      propertyChanges.groupBy(uriMergeSpecification =>
        uriMergeSpecification.replacingURI)
    println(s""" Change Specifications Grouped By Replacing URI : size: ${propertyChangesGroupedByReplacingURI.size}""")
    for ((uriTokeep, uriMergeSpecifications) <- propertyChangesGroupedByReplacingURI)
      removeDuplicatesFromSpec(uriTokeep, uriMergeSpecifications, auxiliaryOutput)

    val outputDir = new File(csvSpecification).getParent
    outputModifiedTurtle(csvSpecification + ".ttl", outputDir)
    outputGraph(auxiliaryOutput, csvSpecification + ".aux.ttl", outputDir)
  }

  /** read CSV file with columns restruc:property & restruc:replacingProperty */
  private def readCSVFile(file: String): URIMergeSpecifications = {
    val graph = run(new FileInputStream(file),
      URI("urn:/owl/transform/"), List())
    println(s"""DuplicateCleanerSpecificationApp:
      file $file:
      graph size: ${graph.size}""")
    val queryString = s"""
         | PREFIX restruc: <http://deductions.github.io/restruc.owl.ttl#>
         | PREFIX rdfs: <${uriFromPrefix("rdfs")}>
         | SELECT ?P ?RP ?LAB ?COMM
         | WHERE {
         |   ?S restruc:replacingProperty ?RP ;
         |     restruc:property ?P .
         |   OPTIONAL {
         |     ?S rdfs:label ?LAB ;
         |        rdfs:comment ?COMM .
         |   }
         | }""".stripMargin
    val variables = Seq("P", "RP", "LAB", "COMM");
    val res = runSparqlSelectNodes(queryString, variables, graph: Rdf#Graph)
    res.map { s =>
      URIMergeSpecification(uriNodeToURI(s(0)), uriNodeToURI(s(1)),
        literalNodeToString(s(2)), literalNodeToString(s(3)))
    }
  }

}