package deductions.runtime.services

import deductions.runtime.utils.Configuration
import org.w3.banana.RDF

import scala.collection.mutable.ArrayBuffer
import deductions.runtime.utils.SaveListener
import deductions.runtime.utils.RDFStoreLocalProvider

trait SaveListenersManager[Rdf <: RDF] {
  val config: Configuration

  private val saveListeners = ArrayBuffer[SaveListener[Rdf]]()

  def addSaveListener(l: SaveListener[Rdf]) = {
    saveListeners += l
  }

  /** TODO add HTTPrequest argument */
  def callSaveListeners(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple])
  (implicit userURI: String, rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]) = {
    
    // TODO move the test to relevant implementation
    if (config.recordUserActions)

      saveListeners.map {
        _.notifyDataEvent(addedTriples, removedTriples, request = deductions.runtime.core.HTTPrequest() )
      }
  }
}