package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfigurationProvider

object UserNamedGraphsDispatcherApp
    extends App
    with DefaultConfigurationProvider
    with ImplementationSettings.RDFCache // RDFModule 
    with UserNamedGraphsDispatcher[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  dispatchToUserNamedGraphs(args(0))
}

trait UserNamedGraphsDispatcher[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
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
      |#DELETE { GRAPH <$inputGraphName> {
      |# ?PER ?P ?O .
      |# ?O ?P1 ?O1.
      |#}}
      |
      |INSERT { GRAPH ?USER_GRAPH {
      | ?PER ?P ?O .
      | ?O ?P1 ?O1.
      | ?S ?P3 ?O3 .
      |}}
      |
      |WHERE { GRAPH <$inputGraphName> {
      |  ?PER a foaf:Person ;
      |       foaf:mbox ?MB ;
      |       ?P ?O .
      |  # direct triples 2 steps from ?PER
      |  OPTIONAL {?O ?P1 ?O1 }
      |  # inverse triples and direct from there (use case pair:hasResponsible)
      |  OPTIONAL {
      |  ?S ?P2 ?PER .
      |  ?S ?P3 ?O3 . }
      |  BIND ( URI(CONCAT('mailto:', ?MB )) AS ?USER_GRAPH )
      |}}
      |""".stripMargin

    val queryOrganizationScheme = s"""
      |${declarePrefix(foaf)}
      |#DELETE { GRAPH <$inputGraphName> {
      |# ?PER ?P ?O .
      |# ?O ?P1 ?O1.
      |#}}
      |
      |INSERT { GRAPH ?ORGA {
      | ?PER ?P ?O .
      | ?O ?P1 ?O1.
      | ?ORGA ?P3 ?O3 .
      |}}
      |
      |WHERE { GRAPH <$inputGraphName> {
      |  ?PER a foaf:Person ;
      |       foaf:mbox ?MB ;
      |       ?P ?O .
      |  # direct triples 2 steps from ?PER
      |  OPTIONAL {?O ?P1 ?O1 }
      |  # inverse triples and direct from there (use case gvoi:hasResponsible)
      |  OPTIONAL {
      |    ?ORGA ?P2 ?PER .
      |    ?ORGA ?P3 ?O3 .
      |    ?ORGA a foaf:Organization .
      |  }
      |}}
      |""".stripMargin
    
   val query = queryOrganizationScheme
    println(s"dispatchToUserNamedGraphs: query $query")
    println(s"dispatchToUserNamedGraphs: inputGraphName $inputGraphName, return status: ${sparqlUpdateQueryTR(query)}")

  }
}