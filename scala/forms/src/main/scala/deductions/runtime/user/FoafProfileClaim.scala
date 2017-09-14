package deductions.runtime.user

import deductions.runtime.core.SemanticController
import deductions.runtime.core.HTTPrequest
import scala.xml.NodeSeq
import org.w3.banana.RDF
import org.w3.banana.RDF

/**
 * Claim a chosen FOAF Profile by associating it with the current foaf:OnlineAccount:
 *
 * <chosenClaimedPerson> foaf:account <UserAccount> .
 */
trait FoafProfileClaim[Rdf <: RDF, DATASET] extends SemanticController
    with UserQueries[Rdf, DATASET] {
  override val featureURI: String = "???"
  override def result(request: HTTPrequest): NodeSeq = {
    val personFromAccount = getPersonFromAccount(request.userId())
    val currentPageIsAfoafPerson: Boolean = ???
    if (currentPageIsAfoafPerson)
      if (personFromAccount == nullURI) {
        // TODO propose to claim current page's identity (foaf:Person)
        <div>
        </div>
      } else
        // TODO link to current page's identity
        <div>
        </div>
    else NodeSeq.Empty
  }
}