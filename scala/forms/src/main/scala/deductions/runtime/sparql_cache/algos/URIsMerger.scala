package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.RDFOps

trait URIsMerger[Rdf <: RDF] {
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  def mergeURIs() = {
    
  }
}