package deductions.apps

import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.SPARQLHelpers
import scala.xml.NodeSeq._ // seqToNodeSeq
import deductions.runtime.core.SemanticController
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFPrefixes

trait ContactsDashboard[Rdf <: RDF, DATASET] extends 
    SPARQLHelpers[Rdf, DATASET]
with RDFPrefixes[Rdf]
with SemanticController {

  import ops._

  override val featureURI: String = fromUri(dbpedia("Contact_manager")) + "/details"
  override def result(request: HTTPrequest): NodeSeq = {
    contactsDashboardHTML( request )
  }

  /**
   * contacts Dashboard:
   *  - statistics
   *  - history (the most recent contacts)
   *  - the contacts I created
   *  - the contacts directly connected to me
   *  - contact recommendations
   */
  def contactsDashboardHTML(request: HTTPrequest): NodeSeq = {
    <div class="raw">
      {
        val person = getPersonFromAccount(request.userId())
        contactsDirectlyConnected(person) }
    </div>
  }

  private def getPersonFromAccount(userId: String): Rdf#Node = {
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

  /** the first circle :) of given person */
  private def contactsDirectlyConnected(person: Rdf#Node): NodeSeq = {
    val personURI = fromUri(nodeToURI(person))
    val queryString = s"""
      ${declarePrefix(foaf)}
      CONSTRUCT {
        <${personURI}> ?P ?V .
      }
      WHERE { GRAPH ?GR {
        <${personURI}> ?P ?V .
        ?V a <${foaf.Person}> .
      }}"""

    println(queryString)

    val tryGraph = sparqlConstructQueryGraph(queryString)

    // TODO call displayResults
    <div class="raw">
      {
        val v = for (
          graph <- tryGraph
        ) yield {
          val vv = for (
            tr <- (getTriples(graph).toList);
            uri = tr.objectt
          ) yield uri
          vv
        }
        v
      }
    </div>
  }
}
