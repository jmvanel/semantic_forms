package deductions.apps

import org.w3.banana.RDF
import scala.xml.NodeSeq

import deductions.runtime.views.MainXmlWithHead
import deductions.runtime.utils.I18NMessages
import deductions.runtime.core.HTTPrequest
import deductions.runtime.user.RegisterPage

/** HTML Front page skeleton for the Contacts SF application */
trait ContactsFrontPage[Rdf <: RDF, DATASET]
extends MainXmlWithHead[Rdf, DATASET]
with ContactsDashboard[Rdf, DATASET]
with RegisterPage[Rdf, DATASET] {

	import ops._

  override val featureURI: String = fromUri(dbpedia("Contact_manager")) //  + "/index"
  override val htmlGenerator = null

  override def result(request: HTTPrequest): NodeSeq = {
    val userid = request.userId()
    val userInfo = displayUser(userid, request)
    val content = contactsDashboardHTML(request)

    mainPage(content, userInfo = userInfo
      , title = "Contacts Mgnt",
      displaySearch = true,
      messages = <p/>,
      httpRequest = request )
	}

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  override def mainPageHeader(implicit lang: String = "en", userInfo: NodeSeq,
      displaySearch: Boolean = true,
      messages: NodeSeq=NodeSeq.Empty): NodeSeq = {
    <header class="col col-sm-12">
      <div class="raw">
        <div class="col col-sm-9">
          <a href="/" title="Open a new Semantic_forms in a new tab." target="_blank">
            <h1>
              { messageI18N("Welcome") }
            </h1>
          </a>
        </div>
        <div class="col col-sm-3">
          { userInfo }
        </div>
      </div>
    </header>
    <div> {
      enterSearchTerm()
    } </div>
    <hr></hr>
  }

}

