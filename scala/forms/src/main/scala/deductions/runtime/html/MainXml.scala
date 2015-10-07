package deductions.runtime.html

import deductions.runtime.utils.I18NMessages
import scala.xml.NodeSeq
import deductions.runtime.views.ToolsPage
import scala.xml.NodeSeq.seqToNodeSeq

trait MainXml extends ToolsPage {

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

  def head(implicit lang: String = "en") = <head></head>

  def linkToToolsPage = <p>
                          <a href="/tools">Tools</a>
                        </p>

  def message(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  def mainPageHeader(implicit lang: String = "en"): NodeSeq = {
    val prefixAV = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"

    <div><h3>{ message("Welcome") }</h3></div>
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/display">
          <div class="form-group">
            <label class="col-md-2 control-label" for="Display">{ message("URI_to_display") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="displayuri" list="start_uris"/>
              <datalist id="start_uris">
                <option label="J.M. Vanel FOAF profile"> http://jmvanel.free.fr/jmv.rdf#me </option>
                <option label="Paris dbpedia.org"> http://dbpedia.org/resource/Paris </option>
                <option label="H. Story FOAF profile"> http://bblfish.net/people/henry/card#me </option>
              </datalist>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" name="Display" value={ message("Display") }/>
              <input class="btn btn-primary" type="submit" name="Edit" value={ message("Edit") }/>
            </div>
            <input type="submit" style="display:none"/>
          </div>
        </form>
      </div>
    </div>
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/wordsearch">
          <div class="form-group">
            <label class="col-md-2 control-label" for="q" title="Search URI whose value (object triple) match given regular expression">
              { message("String_to_search") }
            </label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="q" placeholder={ message("Search_placeholder") }/>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" value={ message("Search") }/>
            </div>
            <input type="submit" style="display:none"/>
          </div>
        </form>
      </div>
    </div>
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/create">
          <div class="form-group">
            <label class="col-md-2 control-label" for="uri">{ message("Create_instance_of") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="uri" placeholder={ message("Paste_ontology") }></input>
              <select class="form-control" type="text" name="uri" list="class_uris">
                <optgroup label="Assemblée Virtuelle">
                  <option label="av:Person"> { prefixAV }Person </option>
                  <option label="av:Organization"> { prefixAV }Organization </option>
                  <option label="av:Project" title="Projet dans ontologie de l'Assemblée Virtuelle">
                    { prefixAV }
                    Project
                  </option>
                  <option label="av:Idea"> { prefixAV }Idea </option>
                  <option label="av:Resource"> { prefixAV }Resource </option>
                </optgroup>
                <optgroup label={ message("Other_vocabs") }>
                  <option label="foaf:Person" selected="selected"> http://xmlns.com/foaf/0.1/Person </option>
                  <option label="doap:Project"> http://usefulinc.com/ns/doap#Project </option>
                  <option label="foaf:Organization"> http://xmlns.com/foaf/0.1/Organization </option>
                  <!-- http://www.w3.org/2002/12/cal/ical#Vevent" // "cal:Vevent" -->
                  <option label="owl:Class"> http://www.w3.org/2002/07/owl#Class </option>
                  <option label="owl:DatatypeProperty"> http://www.w3.org/2002/07/owl#DatatypeProperty </option>
                  <option label="owl:ObjectProperty"> http://www.w3.org/2002/07/owl#ObjectProperty </option>
                </optgroup>
              </select>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" value={ message("Create") }/>
            </div>
            <input type="submit" style="display:none"/>
          </div>
        </form>
      </div>
    </div>
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
