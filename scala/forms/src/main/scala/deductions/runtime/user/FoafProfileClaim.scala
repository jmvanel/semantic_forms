package deductions.runtime.user

import deductions.runtime.core.SemanticController
import deductions.runtime.core.HTTPrequest
import scala.xml.NodeSeq
import org.w3.banana.RDF
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.views.ResultsDisplay
import deductions.runtime.services.ParameterizedSPARQL
import deductions.runtime.utils.I18NMessages

/**
 * HTML UI to claim a chosen FOAF Person profile by associating it with the current foaf:OnlineAccount:
 *
 * <chosenClaimedPerson> foaf:account <UserAccount> .
 */
trait FoafProfileClaim[Rdf <: RDF, DATASET] extends SemanticController
    with UserQueries[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with ParameterizedSPARQL[Rdf, DATASET]
    with ResultsDisplay[Rdf, DATASET] {
  import ops._

  override val featureURI: String = "???"

  private val javascript: xml.Atom[String] = xml.Unparsed("""
    function sendPOST(content) {
      var http = new XMLHttpRequest();
      var url = "/load";
      http.open("POST", url, true);

      //Send the proper header information along with the request
      http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
      http.setRequestHeader("Accept","text/turtle");

      http.onreadystatechange = function() {//Call a function when the state changes.
      if(http.readyState == 4
      // && http.status == 200
      ) {
        alert("Claim FOAF Person profile: " + http.responseText);
      }
}
http.send(content);
}
""")

  override def result(request: HTTPrequest): NodeSeq = {
    profileClaimUI(request)
  }

  def profileClaimUI(request: HTTPrequest): NodeSeq = {
    val currentFocusURI = URI(request.getRDFsubject())
    val currentPageIsAfoafPerson: Boolean =
      currentFocusURI != nullURI &&
      getObjects(allNamedGraph, currentFocusURI, rdf.typ).toSeq.contains(foaf.Person)

    val label = instanceLabelFromTDB(currentFocusURI, request.getLanguage())
    val userId = request.userId()
    val personFromAccount: Rdf#Node = getPersonFromAccount(userId)
    logger.debug( s""">>>>==== profileClaimUI: currentPageIsAfoafPerson $currentPageIsAfoafPerson
      request.userId() '${userId}' ==> personFromAccount <$personFromAccount>
      currentFocusURI <$currentFocusURI>""")
    val realUserId = userId != "" && userId != "anonymous" 
    val accountNotAlreadyAssociatedToAPerson = personFromAccount == nullURI
    // if currentFocusURI is already a foaf:Person attached to account do not display button
    val currentFocusURInotAlreadyAttachedToAccount =
      currentFocusURI != personFromAccount
    if (currentPageIsAfoafPerson) {
    logger.debug( s""">>>> realUserId $realUserId, accountNotAlreadyAssociatedToAPerson $accountNotAlreadyAssociatedToAPerson
        currentFocusURInotAlreadyAttachedToAccount $currentFocusURInotAlreadyAttachedToAccount""")
      if (realUserId &&
          accountNotAlreadyAssociatedToAPerson &&
          currentFocusURInotAlreadyAttachedToAccount
          ) {
        val absoluteURIForUserid = makeAbsoluteURIForSaving(userId)
        // propose to claim current page's identity (foaf:Person)
        val rdfString = s"""
          ${declarePrefix(foaf)}
          <$currentFocusURI> foaf:account <${absoluteURIForUserid}> .
          <$absoluteURIForUserid> foaf:isAccountOf <$currentFocusURI>  .
          """
        logger.debug( s"profileClaimUI: rdfString $rdfString")
        // `` : ECMAScript 6 (ES6)
        <div>
          <script type="text/javascript">
            { javascript }
          </script>
          <button type="button" onclick={ s"sendPOST(`data=$rdfString`);" }>
            <!-- Claim current page's identity:{ label }
            for current user: {userId}
            -->
            { I18NMessages.format("Claim", request.getLanguage(), label, userId) }
          </button>
        </div>
      } else {
        <span>current Page Is A foaf Person but cannot show claim button</span>
        val accountFromPerson = getAccountFromPerson(currentFocusURI.getString)
        if (accountFromPerson != nullURI)
          // link to current page's associated user account
          <div>
            Account for this Person:{
              makeHyperlinkForURI(accountFromPerson, allNamedGraph,
                  request=request )
            }
          </div>
        else
          <span>No account For this Person '{label}' &lt;{currentFocusURI}&gt; </span>
      }
    } else {
      var mess = <span/>
      logger.whenDebugEnabled { mess = <span>current Page Is NOT A foaf Person</span> }
      mess
    }
  }
}
