package deductions.runtime.views

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.{DefaultConfiguration, I18NMessages, RDFPrefixes}

import scala.xml.NodeSeq
import scala.xml.Elem
import java.net.URLEncoder
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import scala.util.Success
import scala.util.Failure
import scala.xml.Text

/**
 * Buttons for loading/display/edit, search, and create;
 * this the default HTML UI before the form
 * ("generic" application)
 */
trait EnterButtons[Rdf <: RDF, DATASET] extends InstanceLabelsInferenceMemory[Rdf, DATASET]
{

  // private lazy val prefixes = new ImplementationSettings.RDFModule with RDFPrefixes[ImplementationSettings.Rdf] with DefaultConfiguration {}
  // import prefixes._
  import ops._

  protected def messageI18N(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  /** Used in ToolsPage
   *  TODO duplicate code with enterSearchTerm() */
  def enterURItoDownloadAndDisplay()(implicit lang: String = "en") = {

    <div class="row sf-margin-top-10">
      <div class="col-xs-12 col-sm-12 col-md-12 col-md-offset-1">

        <form role="form" action="/display#subject">

            <div class="col-xs-2 col-sm-2 col-md-1">
              <label class="control-label" for="Display">{ messageI18N("Display") }</label>
            </div>

            <div class="col-xs-10 col-sm-10 col-md-6">
              <input class="form-control" type="text" name="displayuri" list="start_uris"
              dropzone="copy string:text/plain"
              placeholder={messageI18N("Display_placeholder")}/>
              { rdfStartingPoints }
            </div>


              <div class="col-xs-4 col-sm-4  col-md-1" >
                <input class="form-control btn btn-primary" type="submit" name="Display" value={ messageI18N("Display") }/>
              </div>
              <div class="col-xs-4 col-sm-4 col-md-1">
                <input class="form-control btn btn-primary" type="submit" name="Edit" value={ messageI18N("Edit") }/>
              </div>
              <div class="col-xs-4 col-sm-3 col-md-1">
                <label class="checkbox-inline"> <input type="checkbox" name="tabs" value="true" />Group fields</label>
              </div>

            <!--input type="submit" style="display:none"/-->
        </form>

      </div>
    </div>
  }

  private val rdfStartingPoints: Elem =
    <datalist id="start_uris">
      <option label="J.M. Vanel FOAF profile"> http://jmvanel.free.fr/jmv.rdf#me </option>
      <option label="Paris dbpedia.org"> http://dbpedia.org/resource/Paris </option>
      <option label="H. Story FOAF profile"> http://bblfish.net/people/henry/card#me </option>
    </datalist>

  /** enter Search Term or URI
   *  Used in main page */
  def enterSearchTerm()(implicit lang: String = "en") = {
    val inputMessage = messageI18N("Search_placeholder")
    val classMessage = messageI18N("Class_placeholder")
        <form  role="form" action="/search" class="sf-margin-top-10">
              <!-- TODO ? dropzone="copy" -->
              <input class="sf-search" type="text" id="q" name="q" size="40"
                placeholder={ inputMessage }
                title={ inputMessage }
                list="start_uris"
                />
              <input class="sf-search sfLookup" type="text" name="clas" size="25"
                placeholder={ classMessage }
                title={ classMessage }
                data-rdf-type={fromUri(rdfs.Class)}
                data-rdf-property={fromUri(rdf.typ)}
                />
              { rdfStartingPoints }
            <input class="btn btn-primary" type="submit" value={ messageI18N("Search") }/>
        </form>
  }

  /** suggested Classes For Creation */
  private lazy val suggestedClassesForCreation: Map[String, NodeSeq] = {
    def encode(u: Rdf#URI): String = URLEncoder.encode(fromUri(u), "UTF-8")
    def suggestedClassForCreation(uri: Rdf#URI, lang: String): NodeSeq = {
      <span><a href={
       "/create?uri=" + encode(uri) } >
         {instanceLabelFromTDB(uri, lang)} ({abbreviateTurtle(uri)})</a> -</span>
    }
    val resultTry = wrapInReadTransaction {

      val suggestedClasses = Seq(
          foafForms("personForm"),
          foaf.Organization,
          prefixesMap2("bioc")("Planting"),
          prefixesMap2("nature")("Observation"),
          sioc("Post"),
          sioc("Thread"),
          prefixesMap2("schema")("Event"),
          foaf.Project,
          prefixesMap2("doas")("Software"),
          prefixesMap2("tm")("Task")
          // foaf.Group, prefixesMap2("doap")("Project"), 
          // prefixesMap2("gr")("Offering")
          // prefixesMap2("schema")("CreativeWork")  // Oeuvre
          // prefixesMap2("cco")("Skill")
          // prefixesMap2("owl")("Class")
          // prefixesMap2("owl")("DatatypeProperty")
          // prefixesMap2("owl")("ObjectProperty")
          // prefixesMap2("seeds")("SeedsBatch")
      )
      val resultMap0 = for (lang <- Seq("fr", "en")) yield {
        lang -> {
          val nodes = for (suggestedClass <- suggestedClasses) yield suggestedClassForCreation(suggestedClass, lang)
          val nf: NodeSeq = nodes.flatten
          nf
        } // . flatten
      }
      resultMap0.toMap
    }
    resultTry match{
      case Success(nodesSeq) => nodesSeq
      case Failure(f) =>
        Map(
            "fr" -> <p>{s"Erreur dans les liens de création: $f"}</p>,
            "en" -> <p>{s"Error in creation links: $f"}</p>
        )
    }
  }

  /** Used in main page */
  def enterClassForCreatingInstance()(implicit lang: String = "en"): NodeSeq =

    <div class="row sf-margin-top-10">
      <div class="col-xs-12 col-sm-12 col-md-12 col-md-offset-1">
        <form role="form" action="/create#subject">

            <div class="col-xs-2 col-sm-2 col-md-1">
              <label class=" control-label" for="uri">{ messageI18N("Create_instance_of") }</label>
            </div>

            <div class="col-xs-10 col-sm-10 col-md-6">
              <input class="form-control sfLookup sf-local-rdf-link" type="text" name="uri" placeholder={
                messageI18N("Paste_ontology")
                } dropzone="copy"
                data-rdf-type={fromUri(rdfs.Class)}
                data-rdf-property={fromUri(rdf.typ)}
              ></input>
                  { suggestedClassesForCreation(lang) }
            </div>

            <div class="col-sm-4 col-sm-offset-4 col-md-2 col-md-offset-0">
              <input id="sf-button-create"
                class="form-control btn btn-primary sf-button-create sf-local-rdf-link" type="submit"
                value={ messageI18N("Create") }/>
            </div>
            <input type="text" style="display:none" name="prefill" value="no">no</input>

        </form>
      </div>
    </div>


/*
  val avOptgroup =
    <optgroup label="Assemblée Virtuelle">
      <optgroup label="Acteur">
        <option label="foaf:Person long"> http://www.virtual-assembly.org/ontologies/1.0/forms#PersonForm </option>
        <option label="foaf:Group">                      { foaf.Group ) } </option>
        <option label="foaf:Organization">               { foaf.Organization ) } </option>
      </optgroup>
      <optgroup label="Idée">
        <option label="av:Theme"> { prefixAVontology }Theme </option>
        <option label="Proposition"> { prefixAVontology }Proposition </option>
        <option label="Commentaire"> { prefixAVontology }Comment </option>
      </optgroup>
      <optgroup label="Projet">
        <option label="foaf:Project">                    { foaf.Project } </option>
        <option label="av:InitiativeOrMission"> { prefixAVontology }InitiativeOrMission </option>
        <option label="av:Event"> { prefixAVontology }Event </option>
        <option label="Tâche"> { prefixesMap2("tm")("Task") }</option>
      </optgroup>
      <optgroup label="Ressource">
        <option label="Bien ou service"> { prefixesMap2("gr")("Offering") }</option>
        Logiciel
        <option label="Desc. Of A Software (DOAS)">      { prefixesMap2("doas")("Software") } </option>
        Compétence
        <option label="cco:Skill">                       { prefixesMap2("cco")("Skill") } </option>
        <option label="Document">                        { foaf.Document } </option>
        <option label="Lieu">                            { prefixesMap2("schema")("Place") } </option>
        <option label="Oeuvre">                          { prefixesMap2("schema")("CreativeWork") } </option>
        <option label="Ressource financière">            { prefixesMap2("pair")("FinancialResource") } </option>
        <option label="Ressource naturelle">             { prefixesMap2("pair")("NaturalResources") } </option>
        <option label="av:Resource"> { prefixAVontology }Resource </option>
      </optgroup>
    </optgroup>
    */
}
