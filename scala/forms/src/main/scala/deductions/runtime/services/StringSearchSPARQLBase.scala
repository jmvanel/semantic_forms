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
   private def textQuery(search: String) =
    if (search.length() > 0) {
      val searchStringPrepared = prepareSearchString(search).trim()
      if (config.useTextQuery)
        // include * for Lucene search
        s"?thing text:query ( '$searchStringPrepared*' ) ."
      else
        s"""graph ?gtext {
              ?thing ?P1 ?O1 .
              FILTER ( isLiteral( ?O1) )
              FILTER ( regex( str(?O1), '$searchStringPrepared.*', "i" ) )
            }
            """
    } else ""

  private def classCriterium(classe: String): String = {
    // TODO test performances
    val superclassesSearch = s"""|
         | graph ?g1 {
         |   ?thing a ?sub .
         | }
         | graph ?g2 {
         |   ?sub rdfs:subClassOf* <${expandOrUnchanged(classe)}> .
         | }""".stripMargin

    println( s"""classCriterium: class: "${classe}" """)
    if (classe === "")
      "graph ?g1 { ?thing a ?CLASS . }"
    else
      s"""|
         | graph ?g1 {
         |   ?thing a <${expandOrUnchanged(classe)}> .
         | }""".stripMargin
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
  def queryWithlinksCount(search: String,
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

  /** query With links Count, with or without text query */
  def queryWithlinksCountMap(search: String,
                          classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(form)}
         |${declarePrefix(geo)}
         |CONSTRUCT {
         |  ?thing geo:long ?LONG .
         |  ?thing geo:lat ?LAT .
         |  ?thing rdfs:label ?LAB .
         |} WHERE {
         |  ${textQuery(search)}
         |  ${classCriterium(classe)} .
         |  graph ?grll {
         |    ?thing geo:long ?LONG .
         |    ?thing geo:lat ?LAT .
         |  }
         |  OPTIONAL {
         |  graph ?grlab {
         |    ?thing rdfs:label ?LAB } }
         |  OPTIONAL {
         |  graph ?grlab2 {
         |    ?thing <urn:displayLabel> ?LAB } }
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         |""".stripMargin

  /** query With links Count, with or without text query, returning class */
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

  /** prepare Search String: trim, and replace ' with \' */
  private def prepareSearchString(search: String) = {
    search.trim().replace("'", """\'""")
  }
}
