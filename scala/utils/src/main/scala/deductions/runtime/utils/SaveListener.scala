package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

/** **interface** */
trait SaveListener[Rdf <: RDF] {

  def notifyDataEvent(
    addedTriples: Seq[Rdf#Triple],
    removedTriples: Seq[Rdf#Triple],
    request: HTTPrequest,
    ipAdress: String = "",
    isCreation: Boolean = false)(implicit userURI: String,
    rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]): Unit
}