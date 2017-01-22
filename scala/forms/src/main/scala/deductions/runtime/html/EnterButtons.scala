package deductions.runtime.html

//import deductions.runtime.services.Configuration
import deductions.runtime.utils.I18NMessages
import scala.xml.NodeSeq
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import org.w3.banana.jena.JenaModule

/**
 * Buttons for loading/display/edit, search, and create;
 *  this the default HTML UI before the form
 */
trait EnterButtons //extends Configuration
{

  private lazy val prefixes = new ImplementationSettings.RDFModule with RDFPrefixes[ImplementationSettings.Rdf] with DefaultConfiguration {}
  import prefixes._

  protected def messageI18N(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  def enterURItoDownloadAndDisplay()(implicit lang: String = "en") = {
    <div class="row">
      <div class="col-md-12">
        <form role="form" action="/display">
          <div class="form-group">
            <label class="col-md-2 control-label" for="Display">{ messageI18N("Display") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="displayuri" list="start_uris" dropzone="copy string:text/plain"/>
              <datalist id="start_uris">
                <option label="J.M. Vanel FOAF profile"> http://jmvanel.free.fr/jmv.rdf#me </option>
                <option label="Paris dbpedia.org"> http://dbpedia.org/resource/Paris </option>
                <option label="H. Story FOAF profile"> http://bblfish.net/people/henry/card#me </option>
              </datalist>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" name="Display" value={ messageI18N("Display") }/>
              <input class="btn btn-primary" type="submit" name="Edit" value={ messageI18N("Edit") }/>
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
            <label class="col-md-2 control-label" for="q" title="Search URI whose value (object triple) matches (Lucene search) or (known RDF class)">
              { messageI18N("String_to_search") }
            </label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="q" placeholder={
                messageI18N("Search_placeholder")
              } dropzone="copy"/>
              <input class="form-control" type="text" name="clas" placeholder={ messageI18N("Class_placeholder") }/>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" type="submit" value={ messageI18N("Search") }/>
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
            <label class="col-md-2 control-label" for="uri">{ messageI18N("Create_instance_of") }</label>
            <div class="col-md-6">
              <input class="form-control" type="text" name="uri" placeholder={
                messageI18N("Paste_ontology")
              } dropzone="copy"></input>
              <select class="form-control selectable" type="text" name="uri" list="class_uris">
                <optgroup label="Assemblée Virtuelle">
                  <option label="av:InitiativeOrMission"> { prefixAVontology }InitiativeOrMission  </option>
                  <option label="av:Idea"> { prefixAVontology }Idea </option>
                  <option label="av:Resource"> { prefixAVontology }Resource </option>
                  <option label="av:Event"> { prefixAVontology }Event </option>
                  Theme
Thesis
                </optgroup>
                <optgroup label={ messageI18N("Other_vocabs") }>
                  { suggestedClassesForCreation }
                </optgroup>
              </select>
            </div>
            <div class="col-md-4">
              <input class="btn btn-primary" style="position: relative; top: 18px;" type="submit" value={ messageI18N("Create") }/>
            </div>
            <input type="submit" style="display:none"/>
          </div>
        </form>
      </div>
    </div>

  def suggestedClassesForCreation: NodeSeq = {
    <option label="foaf:Person short" selected="selected"> { forms("personForm") } </option>
    <option label="foaf:Person"> { foaf.Person } </option>
    <option label="foaf:Project">                    { foaf.Project } </option>
    <option label="foaf:Group">                      { foaf.Group } </option>
    <option label="doap:Project"> http://usefulinc.com/ns/doap#Project </option>
    <option label="foaf:Organization">               { foaf.Organization } </option>
    <option label="cco:Skill">                       { prefixesMap2("cco")("Skill") } </option>
    <option label="sioc:Post">                       { sioc("Post") } </option>
    <option label=" cal:Vevent "> http://www.w3.org/2002/12/cal/ical#Vevent </option>
    <option label="owl:Class">                       { prefixesMap2("owl")("Class") } </option>
    <option label="owl:DatatypeProperty">            { prefixesMap2("owl")("DatatypeProperty") } </option>
    <option label="owl:ObjectProperty">              { prefixesMap2("owl")("ObjectProperty") } </option>
    <option label="bioc:Planting">                   { prefixesMap2("bioc")("Planting") } </option>

  }
}
