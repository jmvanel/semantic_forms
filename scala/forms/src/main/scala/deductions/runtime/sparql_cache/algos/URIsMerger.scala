package deductions.runtime.sparql_cache.algos

import org.w3.banana.{RDF, RDFOps}

trait URIsMerger[Rdf <: RDF] {
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  
  def mergeURIs() = {
    
  }
}