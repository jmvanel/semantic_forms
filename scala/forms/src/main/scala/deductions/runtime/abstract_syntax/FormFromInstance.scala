package deductions.runtime.abstract_syntax
import org.w3.banana.RDF
import scala.collection.mutable
import org.apache.log4j.Logger
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.URIOps
import org.w3.banana.XSDPrefix
import org.w3.banana.diesel.toPointedGraphW
import org.w3.banana.RDFOpsModule

/** unused */
trait FormFromInstance[Rdf <: RDF] 
extends RDFOpsModule {
  /** find fields from given Instance subject */
  def fields(subject: Rdf#URI, graph: Rdf#Graph): Seq[Rdf#URI] = {
    subject.predicates.toSeq
  }

}