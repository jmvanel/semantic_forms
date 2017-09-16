package deductions.runtime.user

import deductions.runtime.core.SemanticController
import deductions.runtime.core.HTTPrequest
import scala.xml.NodeSeq
import org.w3.banana.RDF
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory

/**
 * HTML UI to claim a chosen FOAF Person profile by associating it with the current foaf:OnlineAccount:
 *
 * <chosenClaimedPerson> foaf:account <UserAccount> .
 */
trait FoafProfileClaim[Rdf <: RDF, DATASET] extends SemanticController
    with UserQueries[Rdf, DATASET]
with InstanceLabelsInferenceMemory[Rdf, DATASET] {
  import ops._

  override val featureURI: String = "???"

  override def result(request: HTTPrequest): NodeSeq = {
    val uri = URI( request.getRDFsubject() )
    val currentPageIsAfoafPerson: Boolean =
      getObjects(allNamedGraph, (uri), rdf.typ) . toSeq . contains(foaf.Person)
    
    if (currentPageIsAfoafPerson) {
    	val personFromAccount = getPersonFromAccount(request.userId())
      if (personFromAccount == nullURI) {
        // propose to claim current page's identity (foaf:Person)
        val label = instanceLabelFromTDB(uri, request.getLanguage())
        <div>
        		<a href="/load">claim current page's identity: </a>
        </div>
      } else
        // TODO link to current page's identity
        <div>
        </div>
    }
    else NodeSeq.Empty
  }
}