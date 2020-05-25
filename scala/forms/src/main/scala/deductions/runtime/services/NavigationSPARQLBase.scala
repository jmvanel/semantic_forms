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

  /** neighborhood Search SPARQL: like extendedSearchSPARQL + reverse + direct
   *  used in /history?uri= page */
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

  def reverseLinks(search: String, property: String = ""): String = {
    val propertyVariable = property match {
      case p if (p.length() > 0) => s"<$p>"
      case _                     => "?p"
    }
    s"""
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing $propertyVariable <$search> .
         |  }
         |  $countPattern
         |}
         | ORDER BY DESC(?COUNT)
         |""".stripMargin
  }

    /** same as #reverseLinks , but add triples for geo. maps */
    def reverseLinksMaps(search: String): String = s"""
         |${declarePrefix(form)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(geo)}
         |${declarePrefix(foaf)}

         |CONSTRUCT {
         |  ?thing geo:long ?LONG .
         |  ?thing geo:lat ?LAT .
         |  ?thing rdfs:label ?LAB .
         |  ?thing foaf:depiction ?IMG .
         |} WHERE {
         |  graph ?g {
         |    ?thing ?p <${search}> .
         |  }
         |  graph ?gcoord {
         |    ?thing geo:long ?LONG .
         |    ?thing geo:lat ?LAT .
         |  }
         |  OPTIONAL {
         |   graph ?g1 {
         |    ?thing rdfs:label ?LAB } }
         |  OPTIONAL {
         |   graph ?g2 {
         |    ?thing <urn:displayLabel> ?LAB } }
         |
         |  OPTIONAL {
         |   graph ?g3 {
         |    ?thing foaf:depiction ?IMG } }
         |  OPTIONAL {
         |   graph ?g4 {
         |    ?thing foaf:img ?IMG } }
         |
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         |""".stripMargin

  /** list of named graphs matching a string or regex */
  def namedGraphs(containsFilter: Option[String] = None, regex: Option[String] = None): String = {
      // TODO show # of triples
      val containsFilterClause = containsFilter match {
        case Some(pattern) => s"  FILTER ( CONTAINS(STR(?thing),'${pattern}'))"
        case None => ""
      }
      val regexFilterClause = regex match {
        case Some(pattern) => s"  FILTER ( REGEX(STR(?thing),'${pattern}'))"
        case None => ""
      }
      val sparql = s"""
         |SELECT DISTINCT ?thing
         |    # ?CLASS
         |    WHERE {
         |  graph ?thing {
         |    [] ?p ?O .
         |    # TODO: lasts very long with this
         |    # OPTIONAL { ?thing a ?CLASS . }
         |  }
         |  $containsFilterClause
         |  $regexFilterClause
         |}""".stripMargin
      logger.debug( s">>>> namedGraphs(contains=$containsFilter, regex=$regex=> $sparql")
      sparql
  }
}