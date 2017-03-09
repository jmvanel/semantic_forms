package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import java.io.File
import deductions.runtime.services.SPARQLHelpers
import java.io.FileWriter
import deductions.runtime.connectors.CSVImporter
import java.io.FileInputStream
import deductions.runtime.services.DefaultConfiguration

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
object DuplicateCleanerSpecificationApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with RDFStoreLocalJena1Provider
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DuplicateCleaner[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import config._
  import ops._

  override val databaseLocation = "/tmp/TDB" // TODO multi-platform temporary directory
  override val deleteDatabaseLocation = true

  println(s"databaseLocation $databaseLocation")

  duplicateCleanerSpecificationApp()

  ////  functions  ////

  def duplicateCleanerSpecificationApp() = {
    possiblyDeleteDatabaseLocation()
    println( "args" + args.mkString(", ") )
    val args2 = args.map { new File(_).getCanonicalPath }

    loadFilesFromArgs(args2)

    val csvSpecification = args2(0)
    val propertyChanges = readCSVFile(csvSpecification)
    println(s"""DuplicateCleanerSpecificationApp:
      CSV input $csvSpecification,
      Change Specifications: size ${propertyChanges.size}""")
//    println(s"""DuplicateCleanerSpecificationApp: ${propertyChanges.mkString("\n")}""")

    val auxiliaryOutput: Rdf#MGraph = makeEmptyMGraph()

    { // remove Duplicates From Spec (and possibly rename)
      val propertyChangesGroupedByReplacingURI =
        propertyChanges.groupBy(uriMergeSpecification =>
          uriMergeSpecification.replacingURI)
      println(s""" Change Specifications Grouped By Replacing URI : size: ${propertyChangesGroupedByReplacingURI.size}""")
      for ((uriTokeep, uriMergeSpecifications) <- propertyChangesGroupedByReplacingURI)
        if( uriTokeep != nullURI)
          removeDuplicatesFromSpec(uriTokeep, uriMergeSpecifications, auxiliaryOutput)
    }

    { // pure rename From Spec
      val propertyChangesGroupedByReplacedURI =
        propertyChanges.groupBy(uriMergeSpecification =>
          uriMergeSpecification.replacedURI)
      println(s""" Change Specifications Grouped By Replaced URI (for renamings): size: ${propertyChangesGroupedByReplacedURI.size}""")
      wrapInTransaction {
        for (
          (uriTokeep, uriMergeSpecifications) <- propertyChangesGroupedByReplacedURI;
          uriMergeSpecification <- uriMergeSpecifications.headOption if(uriMergeSpecification.isRenaming())
        ) {
          storeLabelWithMergeMarker(uriTokeep, uriMergeSpecification.newLabel,
            merge_marker = "", URI("urn:renamings"))
        }
      }
    }

    val outputDir = new File(csvSpecification).getParent
    outputModifiedTurtle(csvSpecification + ".ttl", outputDir)
    outputGraph(auxiliaryOutput, csvSpecification + ".aux.ttl", outputDir)
  }

  /**
   * read CSV file with columns restruc:property & restruc:replacingProperty
   *  @return URI Merge Specifications, a list of case classe instances
   */
  private def readCSVFile(file: String): URIMergeSpecifications = {
    val graph = run(new FileInputStream(file),
      URI("urn:/owl/transform/"), List())
    println(s"""DuplicateCleanerSpecificationApp:
      file $file:
      graph size: ${graph.size}""")
    val queryString = s"""
         | ${declarePrefix(rdfs)}
         | ${declarePrefix(restruc)}
         | SELECT ?P ?RP ?LAB ?COMM
         | WHERE {
         |   ?S restruc:property ?P .
         |   OPTIONAL { ?S restruc:replacingProperty ?RP }
         |   OPTIONAL { ?S rdfs:label ?LAB }
         |   OPTIONAL { ?S rdfs:comment ?COMM }
         | }""".stripMargin
    val variables = Seq("P", "RP", "LAB", "COMM");
    val res = runSparqlSelectNodes(queryString, variables, graph)
    res.map { s =>
      URIMergeSpecification(uriNodeToURI(s(0)), uriNodeToURI(s(1)),
        literalNodeToString(s(2)), literalNodeToString(s(3)))
    }
  }

}