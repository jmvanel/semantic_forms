package deductions.runtime.views

import deductions.runtime.utils.I18NMessages

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

/** HTML page skeleton for the generic SF application */
trait MainXml extends ToolsPage with EnterButtons {

  /**
   * main Page with a single content (typically a form)
   * Design pattern "Template method"
   */
  def mainPage(content: NodeSeq, userInfo: NodeSeq, lang: String = "en", title: String = "",
               displaySearch: Boolean = true,
               messages: NodeSeq = <p>
                                     New feature considered:
                                     <a href="https://github.com/jmvanel/semantic_forms/issues/152">
                                       Checker for good practices in RDF and OWL #152
                                     </a>
                                   </p>,
               classForContent: String = "container sf-complete-form") = {
    <html>
      <head>
        { head(title)(lang) }
      </head>
      <body>
        {mainPageHeader(lang, userInfo, displaySearch)}
        { messages }
        <div id="appMessages"></div>
        <div class={classForContent}>
        {content}
        </div>
        {pageBottom(lang)}
      </body>
    </html>
      }

  /** HTML head, to be redefined */
  def head(title: String = "")(implicit lang: String = "en"): NodeSeq = <head></head>

  /** page bottom (overridable!) **/
  def pageBottom(lang: String = "en"): NodeSeq = linkToToolsPage(lang)

  def linkToToolsPage(lang: String = "en") =

          <footer class="navbar navbar-default navbar-fixed-bottom sf-footer">
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
  def mainPageHeader(implicit lang: String = "en", userInfo: NodeSeq, displaySearch: Boolean = true): NodeSeq = {

    <header>
      <div class="row">
        <div class="col col-sm-8 col-sm-offset-1">
          <a href="/" title="Open a new Semantic_forms in a new tab." target="_blank">
            <h1>
              {
                messageI18N("Welcome")
              }
            </h1>
          </a>
        </div>
        <div class="col col-sm-3">
          { userInfo }
        </div>
      </div>
    </header>
    <div>
      {
        if (displaySearch) {
          <button type="button" class="btn-primary" data-toggle="collapse" data-target="#collapseDisplay" title={
            messageI18N("Reduce")
          }>
            {
              messageI18N("Reduce")
            }
          </button>
        }
      }
    </div>
    <div class="collapse" id="collapseDisplay">{
      if (displaySearch) {
        enterURItoDownloadAndDisplay() ++
          enterClassForCreatingInstance()
      }
    }</div>
    <div>{ enterSearchTerm() }</div>
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

  /** creation Button for given RDF class */
  def creationButton(classe: String, label: String, formuri: String = ""): NodeSeq =
    <form role="form" action="/create">
      <input type="hidden" name="uri" id="uri" value={ classe }/>
      <input type="hidden" name="formuri" id="formuri" value={
        if (formuri != "") formuri else null
      }/>
      <input type="submit" name="create" id="create" value={ label }/>
    </form>
}
