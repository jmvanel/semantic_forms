package deductions.runtime.utils

import org.w3.banana.RDF
import scala.util.Try

trait SparqlComplements[Rdf <: RDF, DATASET] {
  def executeConstructUnionGraph(
    dataset: DATASET, query: Rdf#ConstructQuery,
    bindings: Map[String, Rdf#Node] = Map()): Try[Rdf#Graph]

  def executeSelectUnionGraph(
        dataset: DATASET, query: Rdf#SelectQuery,
        bindings: Map[String, Rdf#Node]) : Try[Rdf#Solutions]

  /** Executes a Construct query with ARQ Syntax (Jena);
   *  see https://issues.apache.org/jira/browse/JENA-1629,
   *  https://issues.apache.org/jira/browse/JENA-1629 */
  def executeConstructArqSyntax(dataset: DATASET, query: String,
                                bindings: Map[String, Rdf#Node]): Try[DATASET]
}