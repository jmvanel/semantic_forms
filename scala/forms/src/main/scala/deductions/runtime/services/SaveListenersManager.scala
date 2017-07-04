package deductions.runtime.services

import deductions.runtime.semlogs.LogAPI
import deductions.runtime.utils.Configuration
import org.w3.banana.RDF

import scala.collection.mutable.ArrayBuffer
trait SaveListenersManager[Rdf <: RDF] {
  val config: Configuration

  type SaveListener = LogAPI[Rdf]
  val saveListeners = ArrayBuffer[SaveListener]()

  def addSaveListener(l: SaveListener) = {
    saveListeners += l
  }

  def callSaveListeners(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple])(implicit userURI: String) = {
    if (config.recordUserActions)
      saveListeners.map {
        _.notifyDataEvent(addedTriples, removedTriples)
      }
  }
}