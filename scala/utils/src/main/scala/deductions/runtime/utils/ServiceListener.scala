package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

/** **interface** */
trait ServiceListener[Rdf <: RDF, DATASET] {
  /** general callback for HTTP requests */
  def notifyServiceCall(request: HTTPrequest)(implicit userURI: String,
                                              rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET]): Unit
}


trait ServiceListenersManager[Rdf <: RDF, DATASET] {

  import scala.collection.mutable.Set // ArrayBuffer
  private val listeners = Set[ServiceListener[Rdf, DATASET]]()

  def addServiceListener(l: ServiceListener[Rdf, DATASET]) = {
    listeners += l
  }

  def callServiceListeners(
    request: HTTPrequest)(implicit userURI: String,
                          rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET]) = {
    listeners.map {
      _.notifyServiceCall(request)
    }
  }
}