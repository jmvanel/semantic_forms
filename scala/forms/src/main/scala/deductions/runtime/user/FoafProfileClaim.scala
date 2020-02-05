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
    // this URI is actually a foaf:Person
    val currentFocusURI = URI(request.getRDFsubject())
    val currentPageIsAfoafPerson: Boolean =
      currentFocusURI != nullURI &&
      getObjects(allNamedGraph, currentFocusURI, rdf.typ).toSeq.contains(foaf.Person)

    val label = instanceLabelFromTDB(currentFocusURI, request.getLanguage())
    val personFromAccount = getPersonFromAccount(request.userId())
    logger.debug( s">>>>==== profileClaimUI: personFromAccount ${request.userId()} --> <$personFromAccount>")
    if (currentPageIsAfoafPerson) {
      if (request.userId() != "" &&
          request.userId() != "anonymous" &&
          // if currentFocusURI is already a foaf:Person attached to account do not display button
          currentFocusURI != personFromAccount
          ) {
        val absoluteURIForUserid = makeAbsoluteURIForSaving(request.userId())
        // propose to claim current page's identity (foaf:Person)
        val rdfString = s"""
          ${declarePrefix(foaf)}
          <$currentFocusURI> foaf:account <${absoluteURIForUserid}> .
          <$absoluteURIForUserid> foaf:isAccountOf <$currentFocusURI>  .
          """
//        logger.debug( s"profileClaimUI: rdfString $rdfString")
        // `` : ECMAScript 6 (ES6)
        <div>
          <script type="text/javascript">
            { javascript }
          </script>
          <button type="button" onclick={ s"sendPOST(`data=$rdfString`);" }>
            <!-- Claim current page's identity:{ label }
            for current user: {request.userId()}
            -->
            { I18NMessages.format("Claim", request.getLanguage(), label, request.userId()) }
          </button>
        </div>
      } else {
        val accountFromPerson = getAccountFromPerson(request.getRDFsubject())
        if (accountFromPerson != nullURI)
          // link to current page's associated user account
          <div>
            Account for this Person:{
              makeHyperlinkForURI(accountFromPerson, request.getLanguage(), allNamedGraph,
                  request=request )
            }
          </div>
        else
          <span/>
      }
    } else NodeSeq.Empty
  }
}
