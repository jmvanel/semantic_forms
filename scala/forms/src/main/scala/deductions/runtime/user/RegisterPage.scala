package deductions.runtime.user

import org.w3.banana.RDF
import deductions.runtime.services.StringSearchSPARQL
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.html.TableViewModule
import deductions.runtime.html.CreationFormAlgo
import deductions.runtime.services.Configuration

trait RegisterPage[Rdf <: RDF, DATASET]
    extends StringSearchSPARQL[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with TableViewModule[Rdf, DATASET]
    with CreationFormAlgo[Rdf, DATASET]
    with Configuration {

  import ops._

  /** display User stuff in pages */
  def displayUser(userid: String, pageURI: String, pageLabel: String, lang: String = "en") = {
    <div>
      if( needLogin ){
        if (userid != "")
          instanceLabel(Literal(userid), allNamedGraph, lang) //  display user label
        else {
          <div>
            Anonyme
	    -<a href="/claimid?uri={URLEncode.encode(pageURI)}">
        Revendiquer l'identité de cette page:{ pageLabel }
      </a>
            - ou 
	    -<a href="/register">
        Nouveau compte
      </a>
            - ou 
	    -<a href="/searchid">
        Nouveau compte
      </a>
          </div>
        }
      }
      else Text("All rights granted!")
    </div>
  }

  /**
   * action="claimid"
   *  claim identity is made up of foaf:Person edition + entering password
   */
  def claimIdentityAction(uri: String) = {
    val rawForm = htmlFormElem(uri,
      actionURI = "/saveuser",
      actionURI2 = "/saveuser")
    // TODO put the password after name field inside the form
    <p>
      { passWordField }
      { rawForm }
    </p>
  }

  /**
   * action="register"
   *  register from scratch;
   *  new account is made up of foaf:Person creation + entering password
   */
  def registerAction(uri: String) = {
    val rawForm = create(uri //      , actionURI = "/saveuser", // TODO
    //      actionURI2 = "/saveuser"
    )
    // TODO put the password after name field inside the form
    <p>
      { passWordField }
      { rawForm }
    </p>
  }

  val passWordField =
    <div>
      <label>Entrer un mot de passe</label>
      <input type="password" name="password"></input>
    </div>

  /**
   * action="searchid"
   *  search entered Name in TDB
   */
  def searchEnteredNameAction(enteredName: String) = {
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