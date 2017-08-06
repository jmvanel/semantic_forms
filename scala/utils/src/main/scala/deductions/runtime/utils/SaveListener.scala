package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

trait SaveListener[Rdf <: RDF] {

  def notifyDataEvent(
      addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple],
      request: HTTPrequest,
      ipAdress: String="",
      isCreation: Boolean=false): Unit
}