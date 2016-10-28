package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.jena.ImplementationSettings
import java.io.FileInputStream
import deductions.runtime.sparql_cache.algos.CSVFormatter

object DuplicateCleanerSPARQLApp extends App
    with ImplementationSettings.RDFCache
    with DuplicateCleanerSPARQL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with CSVFormatter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  implicit val graph = turtleReader.read(new FileInputStream(owlFile), "").get

  val homologURIGroups = groupBySPARQL(detectMergeableObjectProperties1, graph)

  for ( homologURIGroup <- homologURIGroups ) {
    output(s"Groupe ${homologURIGroup.map{uri=>rdfsLabel(uri, graph)}.mkString(", ")}")
    for (homologURI <- homologURIGroup) {
      val l = formatCSVLine(homologURI, owl.ObjectProperty )
      output( l )
    }
    output("\n\n")
  }
}


/**
 * merge Duplicates; the criterium is given by a SPARQL query
 */
trait DuplicateCleanerSPARQL[Rdf <: RDF, DATASET]
    extends DuplicateCleaner[Rdf, DATASET] {

  /** @return a list of of sets of homolog URI's, to be merged or printed */
  def groupBySPARQL(sparql: String, graph: Rdf#Graph): List[Set[Rdf#Node]] = {

    val res: List[Seq[Rdf#Node]] = runSparqlSelectNodes(
      sparql, List("P1", "P2", "R"), graph)

    // group by ?R URI value
    val groups = res.groupBy { row => row(2) }
    val g1 = groups.values
    val res2 = g1.map { rows =>
      val v = rows.map {
        row =>
          val set = Set(row(0), row(1))
          set
      }
      val w = v.flatten
      w.toSet
    }
    res2.toList
  }

  /**
   * - P1 object property that connects two classes D1 and R,
   * - P2 object property that connects two classes D2 and R
   * - D1 /= R and D1 /= R
   * - D1 /= D2
   */
  val detectMergeableObjectProperties1 = """
    |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    |PREFIX owl: <http://www.w3.org/2002/07/owl#>
    |
    |SELECT ?P1 ?P2 ?R
    |WHERE {
    |
    | ?P1 a owl:ObjectProperty ;
    |       rdfs:domain ?D1 ;
    |       rdfs:range ?R .
    |
    | ?P2 a owl:ObjectProperty ;
    |       rdfs:domain ?D2 ;
    |       rdfs:range ?R .
    |
    | FILTER ( ?P1 != ?P2 )
    | FILTER ( ?D1 != ?R && ?D2 != ?R )
    | FILTER ( ?D1 != ?D2 )
    |}
    """.stripMargin
}
