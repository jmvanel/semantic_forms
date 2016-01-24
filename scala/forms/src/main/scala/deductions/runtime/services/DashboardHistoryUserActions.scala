package deductions.runtime.services

import scala.concurrent.Future
import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import deductions.runtime.dataset.RDFStoreLocalProvider
import scala.xml.NodeSeq

/** Show History of User Actions:
 *  - URI
 *  - type of action: created, displayed, modified;
 *  - user,
 *  - timestamp,
 *  cf https://github.com/jmvanel/semantic_forms/issues/8
 */
trait DashboardHistoryUserActions[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {
  
  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._
 
 /** leverage on Form2HTMLDisplay.createHTMLResourceReadonlyField() */
 def makeTableHistoryUserActions(userURI: String):NodeSeq = {
    <TODO>
    TODO
    </TODO>
  }
}