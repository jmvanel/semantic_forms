package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

trait ServiceListener[Rdf <: RDF] {
  /** general callback for HTTP requests */
  def notifyServiceCall(request: HTTPrequest)(implicit userURI: String,
                                              rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]): Unit
}