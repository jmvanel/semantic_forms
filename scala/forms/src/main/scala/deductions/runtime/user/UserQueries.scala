package deductions.runtime.user

import org.w3.banana.RDF
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFPrefixes

trait UserQueries[Rdf <: RDF, DATASET] extends SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  def getPersonFromAccount(userId: String): Rdf#Node = {
    val queryString = s"""
      ${declarePrefix(foaf)}
      SELECT ?PERSON
      WHERE { GRAPH ?GR {
        ?PERSON <${foaf.OnlineAccount}> <${userId}> .
      }}"""

    println(queryString)
    val list = sparqlSelectQueryVariablesNT(queryString, Seq("?PERSON"))
    list.headOption.getOrElse(Seq()).headOption.getOrElse(nullURI)
  }
}