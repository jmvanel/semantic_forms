package deductions.runtime.sparql_cache.algos

import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDF
import scala.xml.NodeSeq
import deductions.runtime.utils.RDFPrefixes
import java.net.URLEncoder
import scala.xml.Elem

/** print Statistics for given Graph in HTML */
trait StatisticsGraph[Rdf <: RDF] extends RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {
  import ops._

  def formatHTMLStatistics(focus: Rdf#URI, graph: Rdf#Graph,
                           lang: String = "en"): NodeSeq = {
    val triples = getTriples(graph)
    val predsCount = triples.map { trip => trip.predicate }.toList.distinct.size
    val subjects = triples.map { trip => trip.subject }.toList.distinct
    val subjectsCount = subjects.size
    val objectsCount = triples.map { trip => trip.objectt }.toList.distinct.size
    val objectsCount2 = find(graph, focus, ANY, ANY).map { trip => trip.objectt }.toList.distinct.size
    val triplesCount = graph.size

    val classe = getObjects(graph, focus, rdf.typ).toList.map { abbreviateTurtle(_) }.mkString(", ")

    // TODO hyperlinks to subjects, etc
    val subjectsLink = makeHyperlinks(focus, s" $subjectsCount subjects ")
    <p class="sf-statistics">
    RDF document:
      { triplesCount } triples,
      { subjectsLink } ,
      { predsCount }
      predicates,
      { objectsCount }
      objects,
      { objectsCount2 }
      objects from page URI,
      { if (classe != "") "type $classe" }
    </p>
  }

  /** hyperlink to service /sparql-form?query= */
  //  private def makeHyperlinks(nodes: List[Rdf#Node]) = {
  private def makeHyperlinks( graph: Rdf#Node, mess: String): Elem = {
    val sparql = s"""
      ${declarePrefix(prefixesMap2("owl"))}
      CONSTRUCT {?S <urn:is_in_graph> <$graph> . }
      |WHERE {
      |  GRAPH <$graph> {
      |    ?S ?P ?O .
      |} }
      """.stripMargin
    val q = URLEncoder.encode(sparql, "utf-8")
    val href = "/sparql-form?query=" + q
    <a href={ href }>{ mess }</a>
  }

  private def sparqlInstance(classe: String) = s"""
      ${declarePrefix(prefixesMap2("owl"))}
      CONSTRUCT {?S a <$classe> . }
      |WHERE {
      |  GRAPH ?G {
      |    ?S a <$classe> .
      |} }
      """.stripMargin
}