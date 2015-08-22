package deductions.runtime.dataset

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFStore
import org.w3.banana.SparqlOpsModule
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.SparqlEngine

/**
 * abstract RDFStore Local Provider
 */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET] {
  /** NOTE: same design pattern as for XXXModule in Banana */
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]
  implicit val ops: RDFOps[Rdf]
  implicit val sparqlOps: SparqlOps[Rdf]
  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  val dataset: DATASET
  def allNamedGraph: Rdf#Graph
}
