package deductions.runtime.html

import deductions.runtime.utils.I18NMessages
import scala.xml.NodeSeq
import deductions.runtime.views.ToolsPage
import scala.xml.NodeSeq.seqToNodeSeq

trait MainXml extends ToolsPage with EnterButtons {

  /** main Page with a single content (typically a form) */
  def mainPage(content: NodeSeq, userInfo: NodeSeq, lang: String = "en") = {
    <html>
      { head(lang) }
      <body>
        {
          Seq(
            userInfo,
            mainPageHeader(lang),
            content,
            linkToToolsPage)
        }
      </body>
    </html>
  }

  def head(implicit lang: String = "en"): NodeSeq = <head></head>

  def linkToToolsPage =
    <p>
      ---<br/>
      <a href="/tools">Tools</a>
      -<a href="https://github.com/jmvanel/semantic_forms/wiki/User_manual">User Manual</a>
      -<a href="https://github.com/jmvanel/semantic_forms/wiki/Manuel-utilisateur">Manuel utilisateur</a>
      - Powered by
      <a href="https://github.com/jmvanel/semantic_forms/blob/master/scala/forms_play/README.md#play-framework-implementations">
        semantic_forms
      </a>
    </p>

  //  def message(key: String)(implicit lang: String) = I18NMessages.get(key, lang)
  //  val prefixAV = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  def mainPageHeader(implicit lang: String = "en"): NodeSeq = {
    <div><h3>{ message("Welcome") }</h3></div>
    <div> {
      enterURItoDownloadAndDisplay() ++
        enterSearchTerm() ++
        enterClassForCreatingInstance()
    } </div>
  }

  /**
   * main Page with a content consisting of a left panel
   * and a right panel (typically forms)
   */
  def mainPageMultipleContents(contentLeft: NodeSeq,
    contentRight: NodeSeq,
    userInfo: NodeSeq, lang: String = "en") = {
    <html>
      { head(lang) }
      <body>
        {
          Seq(
            userInfo,
            mainPageHeader(lang),

            <div class="content">
              <div class="left-panel">{ contentLeft }</div>
              <div class="right-panel">{ contentRight }</div>
            </div>,

            linkToToolsPage)
        }
      </body>
    </html>
  }
}
