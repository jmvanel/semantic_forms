package deductions.runtime.user

import deductions.runtime.abstract_syntax.{InstanceLabelsInferenceMemory, PreferredLanguageLiteral}
import deductions.runtime.services.html.TriplesViewModule
import deductions.runtime.services.StringSearchSPARQL
import deductions.runtime.services.html.{CreationFormAlgo, TriplesViewModule}
import deductions.runtime.utils.{Configuration, I18NMessages}
import deductions.runtime.views.ResultsDisplay

import org.w3.banana.RDF

import scala.util.{Failure, Success}
import scala.xml.{NodeSeq, Text}
import scala.xml.Elem
import scala.xml.Text
import scala.xml.Node

/** Register HTML Page */
trait RegisterPage[Rdf <: RDF, DATASET]
    extends StringSearchSPARQL[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with TriplesViewModule[Rdf, DATASET]
    with CreationFormAlgo[Rdf, DATASET]
    with ResultsDisplay[Rdf, DATASET]
//    with Configuration
    {

 	val config: Configuration
  import config._
  import ops._

  /** display User information in pages */
  def displayUser(userid: String, pageURI: String, pageLabel: String,
      lang: String = "en"): NodeSeq = {
    <div class="userInfo"> {
      if (needLogin) {
        if (userid != "" && ! userid.startsWith("anonymous") ) {
          val userLabel = wrapInTransaction {
            // link to User profile
        	  makeHyperlinkForURI( URI("user:"+userid), lang, allNamedGraph )
          }
          val displayUserLabel: NodeSeq = userLabel match {
            case Success(lab) => Text(s"${I18NMessages.get("User", lang)}: ") ++ lab
            case Failure(e) => Text(s"No label for user (!?): $e")
          }
          <div>{displayUserLabel} - <a href="/logout">logout</a></div>

        } else {

          /* TODO obsolete code to reivew nd probably remove
           * (needLogin from config is currently true) */

          <div>
            Anonyme
        	-{
              if (pageURI != "") {
                /* TODO not implemented : service /claimid and button in /display page
                 * for a foaf:Person saying "claim id" */
                <a href="/claimid?uri={URLEncode.encode(pageURI)}" title="il ne reste plus qu'à saisir un mot de passe">
                  Revendiquer l'identité de cette page:{ pageLabel }
                </a>
                Text("- ou -")
              }
            }
            <a href="/login#register" title={I18NMessages.get("New_account", lang)} >
            {I18NMessages.get("New_account", lang)}
            </a>
            - ou 
        	-<a href="#" onclick="document.getElementById('q').focus();" title="Peut-être votre identité est déjà enregistrée ici?">
            Chercher mon identité sur ce site
          </a>
          </div>
        }
      } else Text(I18NMessages.get("All_rights", lang))
    } </div>
  }

  /** UNUSED !
   * action="claimid"
   *  claim identity is made up of foaf:Person edition + entering password
   */
  private def claimIdentityAction(uri: String) = {
    val rawForm = htmlFormElem(uri,
      actionURI = "/saveuser",
      actionURI2 = "/saveuser")
    // TODO put the password after name field inside the form
    <p>
      { passWordField }
      { rawForm }
    </p>
  }

  /** UNUSED !
   * action="register"
   *  register from scratch;
   *  new account is made up of foaf:Person creation + entering password
   */
  private def registerAction(uri: String)
//  (implicit graph: Rdf#Graph)
  = {
	  implicit val graph: Rdf#Graph = allNamedGraph
    val rawForm = create(uri //      , actionURI = "/saveuser", // TODO
    //      actionURI2 = "/saveuser"
    )
    // TODO put the password after name field inside the form
    <p>
      { passWordField }
      { rawForm }
    </p>
  }

  private val passWordField =
    <div>
      <label>Entrer un mot de passe</label>
      <input type="password" name="password"></input>
    </div>

  /**
   * action="searchid"
   *  search entered Name in TDB
   */
  private def searchEnteredNameAction(enteredName: String) = {
    // then click on one link to claim the identity
    searchString(enteredName)
  }

  /* val registerPage =
    <p>
      <h1>Créer un compte</h1>
      <form action="searchid">
        Entrer votre nom ou votre courriel
        <input type="text">
        </input>
      </form>
    </p> */
}