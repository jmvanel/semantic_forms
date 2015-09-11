package deductions.runtime.user

import org.w3.banana.RDF
import deductions.runtime.services.StringSearchSPARQL

trait RegisterPage[Rdf <: RDF, DATASET]
    extends StringSearchSPARQL[Rdf, DATASET] {

  val registerPage =
    <p>
      <h1>Créer un compte</h1>
      <form action="searchid">
        Entrer votre nom ou votre courriel
        <input type="text">
        </input>
      </form>
    </p>

  /**
   * action="searchid"
   *  search entered Name in TDB
   */
  def searchEnteredNameAction(enteredName: String) = {
    // click on one link to claim the identity
    searchString(enteredName)
  }

  /** display User stuff in pages */
  def displayUser(userid: String, pageURI: String, pageLabel: String) = {
    <div>
      {
        if (userid != "")
          userid // TODO display user label
        else {
          <div>
            Anonyme
	    -<a href="/claimid?uri={URLEncode.encode(pageURI)}">
        Revendiquer l'identité de cette page: { pageLabel }
      </a>
            - ou 
	    -<a href="/register">
        Nouveau compte
      </a>
          </div>
          // claim identity is made up of foaf:Person edition + entering password
          // new account is made up of foaf:Person creation + entering password
        }
      }
    </div>
  }

  /** action="claimid" */
  def claimIdentityAction(uri: String) = {
	  ???
  }

  /**
   * action="register"
   *  register from scratch
   */
  def registerAction(uri: String) = {
	  registerPage
	  ???
  }
}