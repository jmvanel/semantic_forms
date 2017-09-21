package deductions.runtime.user

import org.w3.banana.RDF
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFPrefixes

trait UserQueries[Rdf <: RDF, DATASET] extends SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  /** get foaf:Person From Account, transactional */
  def getPersonFromAccountTR(userId: String): Rdf#Node = {
    wrapInTransaction {
      getPersonFromAccount(userId)
    } getOrElse (nullURI)
  }

  /** get foaf:Person From Account, NON transactional */
  def getPersonFromAccount(userId: String): Rdf#Node = {
    val queryString = s"""
      ${declarePrefix(foaf)}
      SELECT ?PERSON
      WHERE { GRAPH ?GR {
        ?PERSON <${foaf.account}> <user:${userId}> .
      }}"""
    println(queryString)
    val list = sparqlSelectQueryVariablesNT(queryString, Seq("?PERSON"))
    list.headOption.getOrElse(Seq()).headOption.getOrElse(nullURI)
  }

  def getAccountFromPerson(person: String): Rdf#Node = {
    val queryString = s"""
      ${declarePrefix(foaf)}
      SELECT ?ACCOUNT
      WHERE { GRAPH ?GR {
        <$person> <${foaf.account}> ?ACCOUNT .
      }}"""
    println(queryString)
    val list = sparqlSelectQueryVariablesNT(queryString, Seq("?ACCOUNT"))
    list.headOption.getOrElse(Seq()).headOption.getOrElse(nullURI)
  }
}