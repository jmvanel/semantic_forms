package deductions.runtime.semlogs

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.syntax._
import org.w3.banana.RDFOps

/** API for Semantic Logs */
trait LogAPI[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._

//  trait SaveListener {
    def notifyDataEvent(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple])
    (implicit userURI: String) = Unit
  
//  trait DataEventListener {
////    def notifyCreateEvent(userURI: String, subjectURI: String, classURI: String)
////    def notifyEditEvent(userURI: String, subjectURI: String, propURI: String, objectURI: String)
//  }
}