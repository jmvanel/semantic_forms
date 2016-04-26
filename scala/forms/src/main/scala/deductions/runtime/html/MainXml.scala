package deductions.runtime.html

import deductions.runtime.utils.I18NMessages
import scala.xml.NodeSeq
import deductions.runtime.views.ToolsPage
import scala.xml.NodeSeq.seqToNodeSeq

trait MainXml extends ToolsPage with EnterButtons {

  /** main Page with a single content (typically a form) */
  def mainPage(content: NodeSeq, userInfo: NodeSeq, lang: String = "en", title: String = "") = {
    <html>
      { head(title)(lang) }
      <body>
        {
          Seq(
            userInfo,
            mainPageHeader(lang),
            content,
            pageBottom)
        }
      </body>
    </html>
  }

  def head(title: String = "")(implicit lang: String = "en"): NodeSeq = <head></head>

  /** page bottom (overridable!) **/
  def pageBottom: NodeSeq = linkToToolsPage

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
      Version vendredi 1 avril 2016, 20:14:39 (UTC+0200)
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
   * and a right panel (typically forms);
   *
   * for http://github.com/assemblee-virtuelle/semforms.git,
   * not yet used :(
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

  /** creation Button for given RDF class */
  def creationButton(classe: String, label: String, formuri: String = ""): NodeSeq =
    <form role="form" action="/create">
      <input type="hidden" name="uri" id="uri" value={ classe }/>
      <input type="hidden" name="formuri" id="formuri" value={
        if (formuri != "") formuri else null
      }/>
      <input type="submit" name="create" id="create" value={ label }/>
    </form>

  def javascriptCSSImports: NodeSeq = {
    <!--script src="assets/javascripts/jquery-2.2.0.min.js" type="text/javascript"></script-->
    <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/smoothness/jquery-ui.min.css"/>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js"></script>

    <!-- bootstrap -->
    <link rel="stylesheet" href="assets/stylesheets/bootstrap.min.css"/>
    <link rel="stylesheet" href="assets/stylesheets/bootstrap-theme.min.css"/>
    <script src="assets/javascripts/bootstrap.min.js" type="text/javascript"></script>

    <!--link rel="stylesheet" href="assets/stylesheets/select2.css"/>
      <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/css/select2.min.css" rel="stylesheet"/>
      <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/js/select2.min.js"></script>
      <script src="assets/javascripts/select2.js" type="text/javascript"></script -->
    
    <script src="assets/javascripts/wikipedia.js" type="text/javascript"></script>
    <script src="assets/javascripts/formInteractions.js" type="text/javascript"></script>
    <script src="assets/javascripts/drawgraph.js" type="text/javascript"></script>

    <script src="assets/fluidgraph/js/jquery-2.1.4.min.js"></script>
    <script src="assets/fluidgraph/js/d3.v3.min.js"></script>
    <script src="assets/fluidgraph/js/jquery.mockjax.min.js"></script>
    <script src="assets/fluidgraph/js/FileSaver.min.js"></script>
    <script src="assets/LDP-framework/mystore.js"></script>
    <script src="assets/fluidgraph/js/semantic2.1.2.js"></script>
    <link rel="stylesheet" href="assets/fluidgraph/css/semantic2.1.2.css"/>
    <script src="assets/fluidgraph/js/mockdata.js"></script>
    <script src="assets/fluidgraph/js/init.js"></script>
    <script src="assets/fluidgraph/js/mygraph.js"></script>
    <script src="assets/fluidgraph/js/mynodes.js"></script>
    <script src="assets/fluidgraph/js/mylinks.js"></script>
    <script src="assets/fluidgraph/js/mybackground.js"></script>
    <script src="assets/fluidgraph/js/convert.js"></script>
  }
}
