package deductions.runtime.utils

import org.w3.banana.RDF
import scala.util.Try

trait SparqlComplements[Rdf <: RDF, DATASET] {
  def executeConstructUnionGraph(
    dataset: DATASET, query: Rdf#ConstructQuery,
    bindings: Map[String, Rdf#Node] = Map()): Try[Rdf#Graph]
}