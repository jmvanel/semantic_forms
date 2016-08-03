package deductions.runtime.sparql_cache.algos

import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDF
import scala.xml.NodeSeq

/** print Statistics for given Graph in HTML */
trait StatisticsGraph[Rdf <: RDF] extends RDFHelpers[Rdf] {
  import ops._

  def formatHTMLStatistics(focus: Rdf#URI, graph: Rdf#Graph,
                           lang: String = "en"): NodeSeq = {
    val predsCount = getPredicates(graph, focus).toList.distinct.size
    val subjectsCount = getTriples(graph) . map{ trip => trip.subject } . toList.distinct.size
    val objectsCount  = getTriples(graph) . map{ trip => trip.objectt } . toList.distinct.size
    val objectsCount2 = find(graph, focus, ANY, ANY).map{ trip => trip.objectt } . toList.distinct.size
    val triplesCount = graph.size
    // TODO hyperlinks to subjects, etc
    <p class="statistics">
      { subjectsCount } subjects,
      { triplesCount } triples,
      { predsCount } predicates,
      { objectsCount } objects,
      { objectsCount2 } objects from page URI
    </p>
  }
}