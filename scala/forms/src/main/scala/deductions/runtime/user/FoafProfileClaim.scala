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
        alert(http.responseText);
    }
}
http.send(content);
}
""")

  override def result(request: HTTPrequest): NodeSeq = {
    profileClaimUI(request)
  }

  def profileClaimUI(request: HTTPrequest): NodeSeq = {
    val uri = URI(request.getRDFsubject())
    val currentPageIsAfoafPerson: Boolean =
      getObjects(allNamedGraph, (uri), rdf.typ).toSeq.contains(foaf.Person)

    val label = instanceLabelFromTDB(uri, request.getLanguage())
    val personFromAccount = getPersonFromAccount(request.userId())

    if (currentPageIsAfoafPerson) {
      if (personFromAccount == nullURI) {
        // propose to claim current page's identity (foaf:Person)
        val rdfString = s"""
          ${declarePrefix(foaf)}
          <$uri> foaf:account <user:${request.userId()}> .
          <user:${request.userId()}> foaf:isAccountOf <$uri>  .
          """
        // `` : ECMAScript 6 (ES6)
        <div>
          <script type="text/javascript">
            { javascript }
          </script>
          <button type="button" onclick={ s"sendPOST(`data=$rdfString`);" }>
            Claim current page's identity:{ label }
            for current user: {request.userId()}
          </button>
        </div>
      } else {
            val accountFromPerson  = getAccountFromPerson(request.getRDFsubject())
        // TODO link to current page's identity, if Current page is user identity
        <div>
          Current page's identity:{ label },
          Account for this Person: &lt;{ accountFromPerson }&gt;,
          Person From Account: &lt;{ personFromAccount }&gt;
        </div>
      }
    } else NodeSeq.Empty
  }
}
