package deductions.runtime.user

import org.w3.banana.RDF
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.URIManagement

trait UserQueries[Rdf <: RDF, DATASET] extends SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with URIManagement {

  /** get foaf:Person From Account, transactional */
  def getPersonFromAccountTR(userId: String): Rdf#Node = {
    wrapInTransaction {
      getPersonFromAccount(userId)
    } getOrElse (nullURI)
  }

  /** get foaf:Person From user Id (non URI), NON transactional */
  def getPersonFromAccount(userId: String): Rdf#Node = {
    val absoluteURIForUserid = makeAbsoluteURIForSaving(userId)
    val queryString = s"""
      ${declarePrefix(foaf)}
      SELECT ?PERSON
      WHERE { GRAPH ?GR {
        ?PERSON <${foaf.account}> <${absoluteURIForUserid}> .
      }}"""
    logger.debug(s"getPersonFromAccount: $queryString")
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
//    println(s"getAccountFromPerson: $queryString")
    val list = sparqlSelectQueryVariablesNT(queryString, Seq("?ACCOUNT"))
    list.headOption.getOrElse(Seq()).headOption.getOrElse(nullURI)
  }

  /** that is, a non-anonymous user */
  def isNamedUser(userid: String) = userid != "" && !userid.startsWith("anonymous")
}
