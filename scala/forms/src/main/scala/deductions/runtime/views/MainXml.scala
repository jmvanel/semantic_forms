package deductions.runtime.views

import deductions.runtime.utils.I18NMessages

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

/** HTML page skeleton for the generic SF application */
trait MainXml[Rdf <: RDF, DATASET] extends ToolsPage[Rdf, DATASET] {

  val defaultSiteMessage = <p>
                                     New feature considered:
                                     <a href="https://github.com/jmvanel/semantic_forms/issues/152">
                                       Checker for good practices in RDF and OWL #152
                                     </a>
                                   </p>
  /**
   * main Page with a single content (typically a form)
   * Design pattern "Template method"
   */
  def mainPage(content: NodeSeq, userInfo: NodeSeq,
      title: String = "",
               displaySearch: Boolean = true,
               messages: NodeSeq = defaultSiteMessage,
               headExtra: NodeSeq = NodeSeq.Empty,
               classForContent: String = "container sf-complete-form",
               httpRequest: HTTPrequest) = {
    val lang = httpRequest.getLanguage()
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang={lang} lang={lang}>
      <head>
        { head(title)(lang) }
        { headExtra }
      </head>
      <body class="sf-body">
        {mainPageHeader(lang, userInfo, displaySearch, messages)}
        <div id="appMessages">{httpRequest.appMessages}</div>
        <div class={classForContent}>
        {content}
        </div>
        {pageBottom(lang)}
      </body>
    </html>
      }

  /** HTML <head> tag, to be redefined */
  def head(title: String = "")(implicit lang: String = "en"): NodeSeq = <head></head>

  /** Technical stuff (JS) that needs to be after page <body> */
  def tail() : NodeSeq = NodeSeq.Empty

  /** page bottom (overridable!) **/
  def pageBottom(lang: String = "en"): NodeSeq = linkToToolsPage(lang) ++
    tail()

  def linkToToolsPage(lang: String = "en") =

          <footer id="footer" class="navbar-default navbar-fixed-bottom sf-footer">
            <a href="/tools">{ I18NMessages.get("Tools", lang) }</a> /
            <a href="https://github.com/jmvanel/semantic_forms/wiki/User_manual">User Manual</a> /
            <a href="https://github.com/jmvanel/semantic_forms/wiki/Manuel-utilisateur">Manuel utilisateur</a> /
            { I18NMessages.get("POWERED", lang) }
            <a href="https://github.com/jmvanel/semantic_forms/blob/master/scala/forms_play/README.md#play-framework-implementations">
              semantic_forms
            </a> /
            Version =timestamp=
          </footer>

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  def mainPageHeader(implicit lang: String = "en", userInfo: NodeSeq, displaySearch: Boolean = true,
              messages: NodeSeq=NodeSeq.Empty ): NodeSeq = {

    <div class="sf-userInfo" > { userInfo } </div>

    <header>
        <div class="">
          <a href="/" title="Open a new Semantic_forms in a new tab." target="_blank">
            <p> { messageI18N("New_tab") } </p>
          </a>
        </div>
    </header>
    <div>
      { messages }
      {
        if (displaySearch) {
          <button id="toggleCreate" type="button" class="btn-primary" data-toggle="collapse"
                  data-target="#collapseDisplay" title={ messageI18N("Reduce") }>
            {
              messageI18N("Reduce")
            }
          </button>
        }
      }

      { I18NMessages.get("Expert_Mode", lang) }
      <input id="expertModeCheckbox" type="checkbox" />
      <script type="text/javascript">
        {
          xml.Unparsed("""
            var expertModeCheckbox = document.querySelector('input[id="expertModeCheckbox"]')
            expertModeCheckbox.addEventListener('click', toggleExpertMode )
          """)
        }
      </script>
    </div>
    <div class="collapse" id="collapseDisplay">{
      if (displaySearch) {
        // enterURItoDownloadAndDisplay() ++
        enterClassForCreatingInstance()
      }
    }</div>
    <div class="sf-search-form-container">{ enterSearchTerm() }</div>
    <hr></hr>
  }

  /**
   * main Page with a content consisting of a left panel
   * and a right panel (typically forms);
   *
   * for http://github.com/assemblee-virtuelle/semforms.git,
   * not yet used :(
   */
  private def mainPageMultipleContents(contentLeft: NodeSeq,
    contentRight: NodeSeq,
    userInfo: NodeSeq, lang: String = "en") = {
    <html>
      { head(lang) }
      <body>
        {
          Seq(

            mainPageHeader(lang,userInfo),

            <div class="content">
              <div class="left-panel">{ contentLeft }</div>
              <div class="right-panel">{ contentRight }</div>
            </div>,

            linkToToolsPage(lang))
        }
      </body>
    </html>
  }

  /** (unused) creation Button (non contextual) for given RDF class */
  private def creationButtonNonContextual(classe: String, label: String, formuri: String = ""): NodeSeq =
    <form role="form" action="/create">
      <input type="hidden" name="uri" id="uri" value={ classe }/>
      <input type="hidden" name="formuri" id="formuri" value={
        if (formuri != "") formuri else null
      }/>
      <input type="submit" name="create" id="create" value={ label }/>
    </form>
}
