package deductions.runtime.sparql_cache.algos

import java.net.URLEncoder

import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import org.w3.banana.RDF

import scala.xml.{Elem, NodeSeq}
import deductions.runtime.views.ResultsDisplay

/** print Statistics for given Graph in HTML */
trait StatisticsGraph[Rdf <: RDF, DATASET] extends RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {

  self: ResultsDisplay[Rdf, DATASET] =>

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

    val linkToClasses = getObjects(graph, focus, rdf.typ).toList
//    val classesAsTurtleTerms = linkToClasses.map { abbreviateTurtle(_) }
    val subjectsLink = makeHyperlinkTtoGraphContent(focus, s" $subjectsCount subjects ", subjectsCount)
    <p class="sf-statistics">
    RDF document:
      { triplesCount } triples,
      { subjectsLink } ,
      { predsCount }
      predicates,
      { objectsCount }
      objects,
      { objectsCount2 }
      objects from page URI, type(s)
      {
        for (link <- linkToClasses) yield {
          makeHyperlinkForURI(link, lang, graph)
          // <a href={ link.toString }>{ classesAsTurtleTerms(a) }</a><span>,&nbsp;</span>
        }
      }
    </p>
  }

  /** hyperlink to given graph content with service /sparql-form?query= */
  private def makeHyperlinkTtoGraphContent( graph: Rdf#Node, mess: String, count:Int): Elem = {
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
    val hyperlink: Elem = <a href={ href }>{ mess }</a>
    if (count > 1)
      <b>{ hyperlink }</b>
    else hyperlink
  }

//  private def sparqlInstance(classe: String) = s"""
//      ${declarePrefix(prefixesMap2("owl"))}
//      CONSTRUCT {?S a <$classe> . }
//      |WHERE {
//      |  GRAPH ?G {
//      |    ?S a <$classe> .
//      |} }
//      """.stripMargin
}