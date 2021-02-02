package deductions.runtime.services

import deductions.runtime.utils.Configuration
import org.w3.banana.RDF
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes

import scalaz._
import Scalaz._

/** common code to StringSearchSPARQL and Lookup */
trait StringSearchSPARQLBase[Rdf <: RDF]
    extends RDFPrefixes[Rdf]
    with RDFHelpers[Rdf]
    with SPARQLBase {

  val config: Configuration

   /** fragment of SPARQL for text Query, with or without text:query
    *  see https://jena.apache.org/documentation/query/text-query.html
    * used both in /lookup and /searchword */
   private def textQuery(search: String, unionGraph: Boolean=false) =
    if (search.length() > 0) {
      val searchStringPrepared = prepareSearchString(search).trim()
      if (config.useTextQuery)
        // include * for Lucene search
        s"?thing text:query ( '$searchStringPrepared*' ) ."
      else
        if( unionGraph )
          s"""
              ?thing ?P1 ?O1 .
              FILTER ( isLiteral( ?O1) )
              FILTER ( regex( str(?O1), '$searchStringPrepared.*', "i" ) )
          """
        else
        s"""graph ?gtext {
              ?thing ?P1 ?O1 .
              FILTER ( isLiteral( ?O1) )
              FILTER ( regex( str(?O1), '$searchStringPrepared.*', "i" ) )
            }
            """
    } else ""

  private def classCriterium(classe: String, unionGraph: Boolean = false): String = {
    logger.debug(s"""classCriterium: class( "${classe}" )""")
    if (unionGraph)
      if (classe === "")
        "  ?thing a ?CLASS ."
      else
        s"""|  ?thing a ?sub .
            |  ?sub rdfs:subClassOf* <${expandOrUnchanged(classe)}> .""".stripMargin
    else if (classe === "")
      "graph ?g1 { ?thing a ?CLASS . }"
    else
      s"""|
         | graph ?g1 {
         |   ?thing a <${expandOrUnchanged(classe)}> .
         | }""".stripMargin
  }

  private def themeCriterium(theme: String, unionGraph: Boolean = false): String = {
    if (theme . startsWith("http"))
        s"  ?thing ?p_dbpedia <$theme> ."
        else if (theme != "")
        s"  ?thing ?p_dbpedia <${dbpedia.prefixIri}$theme> ."
      else
        ""
  }

  private def classVariableInSelect(classe: String): String = {
    if (classe === "")
      "?CLASS"
    else
      ""
  }

  /** UNUSED - No Pre computing of links */
  private def queryWithlinksCountNoPrefetch(search: String,
                                    classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
         |  ${textQuery(search)}
         |  graph ?g {
         |    ?thing ?p ?o .
         |  }
         |  ${classCriterium(classe)} .
         |}
         |GROUP BY ?thing
         |ORDER BY DESC(?count)
         |LIMIT 10
         |""".stripMargin

  /** query With links Count, with or without text query */
  def queryWithlinksCount(
    search: String,
    classe: String = "",
    theme: String = ""): String =
    s"""
    |# queryWithlinksCount() search "$search" class <$classe>
    |${declarePrefix(text)}
    |${declarePrefix(rdfs)}
    |${declarePrefix(form)}
    |${declarePrefix(dbo)}
    |SELECT DISTINCT ?thing ?COUNT WHERE {
    |  ${textQuery(search, unionGraph=false)}
    |  GRAPH <urn:x-arq:UnionGraph> {
    |    ${classCriterium(classe, unionGraph=true)}
    |    ${themeCriterium(theme, unionGraph=true)}
    |    $countPattern
    |    $excludePerson
    |  # $excludePlace
    |  }
    |}
    |ORDER BY DESC(?COUNT)
    |""".stripMargin

  /** query With links Count, with or without text query - UNUSED */
  private def queryWithlinksCountNoUnionGraph(search: String,
                          classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing ?COUNT WHERE {
         |  ${textQuery(search)}
         |  ${classCriterium(classe)} .
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         |LIMIT 10
         |""".stripMargin

  /** query With links Count, with or without text query
   *  TODO duplicate stuff with NavigationSPARQLBase.reverseLinksMaps */
  def queryWithlinksCountMap(search: String,
                          classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(form)}
         |${declarePrefix(geo)}
         |${declarePrefix(foaf)}
         |CONSTRUCT {
         |  ?thing geo:long ?LONG .
         |  ?thing geo:lat ?LAT .
         |  ?thing rdfs:label ?LAB .
         |  ?thing foaf:depiction ?IMG .
         |} WHERE {
         |  ${textQuery(search)}
         |  ${classCriterium(classe)} .
         |  graph ?grll {
         |    ?thing geo:long ?LONG .
         |    ?thing geo:lat ?LAT .
         |  }
         |  OPTIONAL {
         |   graph ?grlab {
         |    ?thing rdfs:label ?LAB } }
         |  OPTIONAL {
         |   graph ?grlab2 {
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

  /** query With links Count, with or without text query, returning class 
   *  UNUSED */
  private def queryWithlinksCountAndClass(search: String,
                          classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing ?COUNT ${classVariableInSelect(classe)} WHERE {
         |  ${textQuery(search)}
         |  ${classCriterium(classe)} .
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         |LIMIT 10
         |""".stripMargin

  // UNUSED
  private def queryWithoutlinksCount(search: String,
                             classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing WHERE {
         |  ${textQuery(search)}
         |  graph ?g {
         |    ?thing ?p ?o .
         |  }
         |  ${classCriterium(classe)} .
         |}
         |LIMIT 15
         |""".stripMargin

  /** prepare Search String: trim, and replace ' with \' ,
   *  _ by a space */
  private def prepareSearchString(search: String) = {
    search.trim().replace("'", """\'""").replace("_", " ")
  }
}
