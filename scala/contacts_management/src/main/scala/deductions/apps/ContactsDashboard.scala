package deductions.apps

import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.SPARQLHelpers
import scala.xml.NodeSeq._ // seqToNodeSeq

trait ContactsDashboard extends ImplementationSettings.RDFCache
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  /**
   * contacts Dashboard:
   *  - statistics
   *  - history (the most recent contacts)
   *  - the contacts I created
   *  - the contacts directly connected to me
   *  - contact recommendations
   */
  def contactsDashboardHTML(): NodeSeq = {
    <div class="raw">
    </div>
  }

  /** the first circle :) */
  def contactsDirectlyConnected(person: Rdf#Node): NodeSeq = {
    val personURI = fromUri(nodeToURI(person))
    val queryString = s"""
      ${declarePrefix(foaf)}
      CONSTRUCT {
        <${personURI}> ?P ?V .
      }
      WHERE { GRAPH ?GR {
        <${personURI}> ?P ?V .
        ?V a ${foaf.Person}.
      }}"""
    val tryGraph = sparqlConstructQueryGraph(queryString)

    // TODO call displayResults
    <div class="raw">
      {
        val v = for (
          graph <- tryGraph
        ) yield {
           val vv = for (
        		 tr <- ( getTriples(graph) . toList);
             uri = tr.objectt
             ) yield uri
        }
      }
    </div>
  }
}
