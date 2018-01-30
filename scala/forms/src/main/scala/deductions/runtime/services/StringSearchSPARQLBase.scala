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
        s"""graph ?g {
              ?thing ?P1 ?O1 .
              FILTER ( isLiteral( ?O1) )
              FILTER ( regex( str(?O1), '$searchStringPrepared.*', "i" ) )
            }
            """
    } else ""

  private def classCriterium(classe: String): String = {
		println(
		  s"""classCriterium: classe: "${classe}" """)
    if (classe === "")
      "?CLASS"
    else
      "<" + expandOrUnchanged(classe) + ">"
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
         |  graph ?g {
         |    ${textQuery(search)}
         |    ?thing ?p ?o .
         |    ?thing a ${classCriterium(classe)} .
         |  }
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
         |  graph ?g1 {
         |    ?thing a ${classCriterium(classe)} .
         |  }
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
         |  graph ?g0 {
         |    ?thing a ${classCriterium(classe)} .
         |  }
         |  graph ?grll {
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
         |""".stripMargin

  /** query With links Count, with or without text query, returning class */
  def queryWithlinksCountAndClass(search: String,
                          classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |${declarePrefix(form)}
         |SELECT DISTINCT ?thing ?COUNT ${classVariableInSelect(classe)} WHERE {
         |  graph ?g {
         |    ${textQuery(search)}
         |    ?thing a ${classCriterium(classe)} .
         |  }
         |  $countPattern
         |}
         |ORDER BY DESC(?COUNT)
         |LIMIT 10
         |""".stripMargin

  // UNUSED
  def queryWithoutlinksCount(search: String,
                             classe: String = "") = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ${textQuery(search)}
         |    ?thing ?p ?o .
         |    ?thing a ${classCriterium(classe)} .
         |  }
         |}
         |LIMIT 15
         |""".stripMargin

  /** prepare Search String: trim, and replace ' with \' */
  private def prepareSearchString(search: String) = {
    search.trim().replace("'", """\'""")
  }
}
