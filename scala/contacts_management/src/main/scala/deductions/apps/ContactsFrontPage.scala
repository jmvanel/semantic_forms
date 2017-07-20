package deductions.apps

import scala.xml.NodeSeq
import deductions.runtime.views.MainXml

import deductions.runtime.utils.I18NMessages

/** HTML Front page skeleton for the Contacts SF application */
trait ContactsFrontPage extends MainXml with ContactsDashboard {

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  override def mainPageHeader(implicit lang: String = "en", userInfo: NodeSeq, displaySearch: Boolean = true): NodeSeq = {
    <header class="col col-sm-12">
      <div class="raw">
        <div class="col col-sm-9">
          <a href="/" title="Open a new Semantic_forms in a new tab." target="_blank">
            <h1>
           {
              messageI18N("Welcome")
              }
            </h1>
          </a>
        </div>
        <div class="col col-sm-3">
          {userInfo}
        </div>
      </div>

    </header>
    <div> {
        enterSearchTerm() ++
        contactsDashboardHTML()
    } </div>
    <hr></hr>
  }

}

