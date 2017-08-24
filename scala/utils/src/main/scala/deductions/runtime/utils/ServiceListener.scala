package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

trait ServiceListener[Rdf <: RDF] {
  /** general callback for HTTP requests */
  def notifyServiceCall(request: HTTPrequest)(implicit userURI: String,
                                              rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]): Unit
}

import scala.collection.mutable.ArrayBuffer

trait ServiceListenersManager[Rdf <: RDF] {

  private val saveListeners = ArrayBuffer[ServiceListener[Rdf]]()

  def addServiceListener(l: ServiceListener[Rdf]) = {
    saveListeners += l
  }

  def callServiceListeners(
    request: HTTPrequest)(implicit userURI: String,
                          rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]) = {
    saveListeners.map {
      _.notifyServiceCall(request)
    }
  }
}