package deductions.runtime.services

import deductions.runtime.utils.Configuration
import org.w3.banana.RDF

import scala.collection.mutable.ArrayBuffer
import deductions.runtime.utils.SaveListener

trait SaveListenersManager[Rdf <: RDF] {
  val config: Configuration

//    type SaveListener = LogAPI[Rdf]
  val saveListeners = ArrayBuffer[SaveListener[Rdf]]()

  def addSaveListener(l: SaveListener[Rdf]) = {
    saveListeners += l
  }

  def callSaveListeners(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple])(implicit userURI: String) = {
    if (config.recordUserActions)
      saveListeners.map {
        _.notifyDataEvent(addedTriples, removedTriples, request = deductions.runtime.core.HTTPrequest() )
      }
  }
}