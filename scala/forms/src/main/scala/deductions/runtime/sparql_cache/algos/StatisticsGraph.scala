package deductions.runtime.sparql_cache.algos

import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDF
import scala.xml.NodeSeq

/** print Statistics for given Graph in HTML */
trait StatisticsGraph[Rdf <: RDF] extends RDFHelpers[Rdf] {
  import ops._

  def formatHTMLStatistics(focus: Rdf#URI, graph: Rdf#Graph,
                           lang: String = "en"): NodeSeq = {
    val predsCount = getPredicates(graph, focus).size
    val subjectsCount = find(graph, focus, ANY, ANY).size
    val triplesCount = graph.size
    <p class="statistics">
      { subjectsCount } subjects,
      { triplesCount } triples,
      { predsCount } predicates
    </p>
  }
}