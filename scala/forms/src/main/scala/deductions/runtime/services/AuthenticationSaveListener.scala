package deductions.runtime.services

import org.w3.banana.RDF
import deductions.runtime.utils.SaveListener
import deductions.runtime.core.HTTPrequest

/** replacement of authenticate in Auth.scala */
trait AuthenticationSaveListener[Rdf <: RDF, DATASET]
extends SaveListener[Rdf]
with Authentication[Rdf, DATASET]
{

  import ops._
  
  override def notifyDataEvent(
      addedTriples: Seq[Rdf#Triple],
      removedTriples: Seq[Rdf#Triple],
      request: HTTPrequest,
      ipAdress: String="",
      isCreation: Boolean=false): Unit = {
    // form: type: Authentication, fields: userid, password, confirmPassword
//    for(tr <- addedTriples) {
//      tr.predicate match {
//        case forms("userid") =>
//        case forms("password") =>
//        case forms("confirmPassword") =>         
//      }
//    }

    		  makeGraph(addedTriples)
//    checkLogin(userid, password)
    ???
  }
}