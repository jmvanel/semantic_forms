package deductions.runtime.services

import org.w3.banana.RDF
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers0

trait NavigationSPARQLBase[Rdf <: RDF]
  extends RDFPrefixes[Rdf]
  with RDFHelpers0[Rdf] {

  def extendedSearchSPARQL(search: String) = s"""
       |# ${declarePrefix(foaf)}
       |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
       | graph ?g {
       |    # "backward" links distance 2
       |    ?TOPIC ?PRED <$search> .
       |    ?thing ?PRED2  ?TOPIC .
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "forward-backward" links distance 2
       |    <$search> ?PRED3 ?TOPIC2 .
       |    ?thing ?PRED4 ?TOPIC2 .
       |  }
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "forward" links distance 2
       |    <$search> ?PRED41 ?TOPIC3 .
       |    ?TOPIC3 ?PRED5 ?thing .
       |  }
       | }
       | OPTIONAL {
       |  graph ?g {
       |    # "backward-forward" links distance 2
       |    ?TOPIC4 ?PRED6 <$search> .
       |    ?TOPIC4 ?PRED7 ?thing . 
       |  }
       | }
       |}
       |GROUP BY ?thing
       |ORDER BY DESC(?count)
       """.stripMargin

  def reverseLinks(search: String) = s"""
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p <${search}> .
         |  }
         |  $countPattern
         |}
         | ORDER BY DESC(?COUNT)
         |""".stripMargin

  /** TODO pasted :( */
  private val countPattern =
    """|  OPTIONAL {
         |   graph ?grCount {
         |    ?thing form:linksCount ?COUNT.
         |  } }""".stripMargin
}