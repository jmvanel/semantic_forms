package deductions.runtime.semlogs

import org.w3.banana.{RDF, RDFOps}

/** API for Semantic Logs */
trait LogAPI[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]

//  trait SaveListener {
    def notifyDataEvent(addedTriples: Seq[Rdf#Triple], removedTriples: Seq[Rdf#Triple],
        ipAdress: String="", isCreation: Boolean=false )
    (implicit userURI: String) = Unit
  
//  trait DataEventListener {
////    def notifyCreateEvent(userURI: String, subjectURI: String, classURI: String)
////    def notifyEditEvent(userURI: String, subjectURI: String, propURI: String, objectURI: String)
//  }
}