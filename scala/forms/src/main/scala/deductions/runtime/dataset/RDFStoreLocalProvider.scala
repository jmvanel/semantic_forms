package deductions.runtime.dataset

import scala.util.Try

import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFStore

/**
 * abstract RDFStore Local Provider
 * NOTE: same design pattern as for XXXModule in Banana
 */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET] extends RDFOpsModule {
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]
  //  type DATASET
  val dataset: DATASET
  def allNamedGraph: Rdf#Graph
}
