package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF 
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.services.SPARQLHelpers

trait UserNamedGraphsDispatcher[Rdf <: RDF, DATASET] extends SPARQLHelpers[Rdf, DATASET]
with RDFHelpers[Rdf] {

  def dispatchToUserNamedGraphs(inputGraphName: String) = {
    val queryUserScheme = s"""
      |${declarePrefix(foaf)}
      |DELETE { GRAPH <$inputGraphName> {
      | ?PER ?P ?O .
      |}}
      |
      |INSERT { GRAPH ?USER_GRAPH {
      | ?PER ?P ?O .
      |}}
      |
      |WHERE { GRAPH <$inputGraphName> {
      |  ?PER a foaf:Person ;
      |       foaf:firstName ?FN ;
      |       foaf:lastName  ?LN ;
      |       ?P ?O .
      |       BIND (REPLACE(CONCAT('user:', ?FN, '_', ?LN), ' ', '_') AS ?USER_GRAPH)
      |}}
      |""".stripMargin

    val queryMailtoScheme = s"""
      |${declarePrefix(foaf)}
      |DELETE { GRAPH <$inputGraphName> {
      | ?PER ?P ?O .
      |}}
      |
      |INSERT { GRAPH ?USER_GRAPH {
      | ?PER ?P ?O .
      |}}
      |
      |WHERE { GRAPH <$inputGraphName> {
      |  ?PER a foaf:Person ;
      |       foaf:mbox ?MB ;
      |       ?P ?O .
      |       BIND (CONCAT('mailto:', ?MB), ' ', '_') AS ?USER_GRAPH)
      |}}
      |""".stripMargin

    println(s"dispatchToUserNamedGraphs: ${sparqlUpdateQueryTR(queryMailtoScheme)}")

  }
}