package deductions.runtime.html

import deductions.runtime.services.Configuration
import deductions.runtime.utils.I18NMessages
import scala.xml.NodeSeq

/**
 * Buttons for loading/display/edit, search, and create;
 *  this the default HTML UI before the form
 */
trait EnterButtons extends Configuration {

  def message(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  def enterURItoDownloadAndDisplay()(implicit lang: String = "en") = {
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/display">
          <div class="form-group">
            <label class="col-md-2 control-label" for="Display">{ message("URI_to_display") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="displayuri" list="start_uris" dropzone="copy string:text/plain"/>
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
  }

  def enterSearchTerm()(implicit lang: String = "en") = {
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/wordsearch">
          <div class="form-group">
            <label class="col-md-2 control-label" for="q" title="Search URI whose value (object triple) match given regular expression">
              { message("String_to_search") }
            </label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="q" placeholder={
                message("Search_placeholder")
              } dropzone="copy"/>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" value={ message("Search") }/>
            </div>
            <input type="submit" style="display:none"/>
          </div>
        </form>
      </div>
    </div>
  }

  def enterClassForCreatingInstance()(implicit lang: String = "en") =
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/create">
          <div class="form-group">
            <label class="col-md-2 control-label" for="uri">{ message("Create_instance_of") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="uri" placeholder={
                message("Paste_ontology")
              } dropzone="copy"></input>
              <select class="form-control" type="text" name="uri" list="class_uris">
                <optgroup label="Assemblée Virtuelle">
                  <option label="av:Person"> { prefixAVontology }Person </option>
                  <option label="av:Organization"> { prefixAVontology }Organization </option>
                  <option label="av:Project" title="Projet dans ontologie de l'Assemblée Virtuelle">
                    { prefixAVontology }
                    Project
                  </option>
                  <option label="av:Idea"> { prefixAVontology }Idea </option>
                  <option label="av:Resource"> { prefixAVontology }Resource </option>
                </optgroup>
                <optgroup label={ message("Other_vocabs") }>
                  { suggestedClassesForCreation }
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

  def suggestedClassesForCreation: NodeSeq = {
    <option label="foaf:Person" selected="selected"> http://xmlns.com/foaf/0.1/Person </option>
    <option label="doap:Project"> http://usefulinc.com/ns/doap#Project </option>
    <option label="foaf:Organization"> http://xmlns.com/foaf/0.1/Organization </option>
    <option label="sioc:Post"> http://rdfs.org/sioc/ns#Post </option>
    <option label=" cal:Vevent "> http://www.w3.org/2002/12/cal/ical#Vevent </option>
    <option label="owl:Class"> http://www.w3.org/2002/07/owl#Class </option>
    <option label="owl:DatatypeProperty"> http://www.w3.org/2002/07/owl#DatatypeProperty </option>
    <option label="owl:ObjectProperty"> http://www.w3.org/2002/07/owl#ObjectProperty </option>
  }
}
