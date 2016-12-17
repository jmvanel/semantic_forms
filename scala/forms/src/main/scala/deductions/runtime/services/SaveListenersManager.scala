package deductions.runtime.services

import scala.collection.mutable.ArrayBuffer

import org.w3.banana.RDF

import deductions.runtime.semlogs.LogAPI

trait SaveListenersManager[Rdf <: RDF]
//    extends Configuration
    {
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