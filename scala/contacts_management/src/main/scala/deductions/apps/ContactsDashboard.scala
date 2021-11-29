package deductions.apps

import scala.xml.NodeSeq
import scala.xml.NodeSeq._ // seqToNodeSeq

import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.core.SemanticController
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.user.UserQueries
import deductions.runtime.views.ResultsDisplay
import scala.util.Success
import scala.util.Failure
import scala.xml.Text

trait ContactsDashboard[Rdf <: RDF, DATASET] extends 
    SPARQLHelpers[Rdf, DATASET]
with RDFPrefixes[Rdf]
with UserQueries[Rdf, DATASET]
with deductions.runtime.services.ParameterizedSPARQL[Rdf,DATASET]
with ResultsDisplay[Rdf, DATASET]
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
        val person = getPersonFromAccountTR(request.userId)
        println(s">>>>>>>>>>> person $person")
        contactsDirectlyConnected(person) }
    </div>
  }

  /** the first circle :) of given person */
  private def contactsDirectlyConnected(person: Rdf#Node): NodeSeq = {
    val personURI = fromUri(nodeToURI(person))
    val queryString = s"""
      ${declarePrefix(foaf)}
      CONSTRUCT {
        <${personURI}> ?P ?V .
      }
      WHERE {
       GRAPH ?GR {
        <${personURI}> ?P ?V .
       }
       GRAPH ?GR1 {
        ?V a <${foaf.Person}> .
       }
      }"""

    println(s"contactsDirectlyConnected: $queryString")

    val tryGraph = sparqlConstructQueryGraph(queryString)
    println(s"contactsDirectlyConnected: $tryGraph")

    <div class="raw">
<pre>
   * contacts Dashboard:
   *  - statistics
   *  - history (the most recent contacts)
   *  - the contacts I created
   *  - the contacts directly connected to me
   *  - contact recommendations
</pre>
      <h3>Contacts Directly Connected to me</h3>
      {
        val v = for (
          graph <- tryGraph
        ) yield {
          val uris = for (
            tr <- (getTriples(graph).toList);
            uri = tr.objectt
          ) yield uri

          wrapInTransaction(
            displayResults(uris, graph = allNamedGraph, request=HTTPrequest() ))
        }
        val tryXHTML = v . flatten
        tryXHTML match {
          case Success(html) => html
          case Failure(f) => Text(s"contactsDirectlyConnected: $f")
        }
      }
      <h3>Contacts Directly Connected from me</h3>
    </div>
  }
}
