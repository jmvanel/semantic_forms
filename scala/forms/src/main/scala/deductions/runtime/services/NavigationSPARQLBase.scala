package deductions.runtime.services

import org.w3.banana.RDF
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.RDFHelpers0

trait NavigationSPARQLBase[Rdf <: RDF]
  extends RDFPrefixes[Rdf]
  with RDFHelpers0[Rdf]
  with SPARQLBase {

  def extendedSearchSPARQL(search: String) = s"""
       |SELECT DISTINCT ?thing (COUNT(*) as ?count)
       |WHERE {
       |
       | ${pathLength2(search)}
       |}
       |GROUP BY ?thing
       |ORDER BY DESC(?count)
       """.stripMargin

  /** neighborhood Search SPARQL: like extendedSearchSPARQL + reverse + direct */
  def neighborhoodSearchSPARQL(search: String) = s"""
       |SELECT DISTINCT ?thing 
       |# (COUNT(*) as ?count)
       |WHERE {
       |
       | ${pathLength2(search)}
       |
       | # reverse links
       | UNION {
       |  graph ?gb {
       |    ?thing ?PREDREV <${search}> .
       |  }
       | }
       | # direct links
       | UNION {
       |  graph ?gf {
       |    <${search}> ?PREDDIRECT ?thing .
       |    filter( isURI(?thing) )
       |  }
       | }
       |}
       |GROUP BY ?thing
       |ORDER BY DESC(?count)
       """.stripMargin

  private def pathLength2(search: String) = s"""
       | {
       |  graph ?gbb {
       |    # "backward" links distance 2
       |    ?TOPIC ?PRED <$search> .
       |    ?thing ?PRED2  ?TOPIC .
       | } }
       | UNION {
       |  graph ?gfb {
       |    # "forward-backward" links distance 2
       |    <$search> ?PRED3 ?TOPIC2 .
       |    ?thing ?PRED4 ?TOPIC2 .
       |  }
       | }
       | UNION {
       |  graph ?gff {
       |    # "forward" links distance 2
       |    <$search> ?PRED41 ?TOPIC3 .
       |    ?TOPIC3 ?PRED5 ?thing .
       |  }
       | }
       | UNION {
       |  graph ?gbf {
       |    # "backward-forward" links distance 2
       |    ?TOPIC4 ?PRED6 <$search> .
       |    ?TOPIC4 ?PRED7 ?thing . 
       |  }
       | }""".stripMargin
       
  def reverseLinks(search: String): String = s"""
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p <${search}> .
         |  }
         |  $countPattern
         |}
         | ORDER BY DESC(?COUNT)
         | LIMIT 150
         |""".stripMargin

    /** same as #reverseLinks , but add triples for geo. maps */
    def reverseLinksMaps(search: String): String = s"""
         |${declarePrefix(form)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(geo)}
         |CONSTRUCT {
         |  ?thing geo:long ?LONG .
         |  ?thing geo:lat ?LAT .
         |  ?thing rdfs:label ?LAB .
         |} WHERE {
         |  graph ?g {
         |    ?thing ?p <${search}> .
         |    ?thing geo:long ?LONG .
         |    ?thing geo:lat ?LAT .
         |  }
         |  OPTIONAL {
         |  graph ?g1 {
         |    ?thing rdfs:label ?LAB } }
         |  OPTIONAL {
         |  graph ?g2 {
         |    ?thing <urn:displayLabel> ?LAB } }
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         | LIMIT 150
         |""".stripMargin
}