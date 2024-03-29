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
import org.w3.banana.Prefix

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

  /** HTML to enter Search Term or URI
   *  Used in main page */
  def enterSearchTerm()(implicit lang: String = "en") = {
    val inputMessage = messageI18N("Search_placeholder")
    val classMessage = messageI18N("Class_placeholder")
    val dbpediaMessage = messageI18N("dbpedia_placeholder")
        <form  role="form" action="/search" class="sf-margin-top-10 sf-search-form">
              <!-- enterSearchTerm() TODO ? dropzone="copy" -->
              <input class="sf-search sfLookup" type="text" id="q" name="q" size="40"
                placeholder={ inputMessage }
                title={ inputMessage }
                list="start_uris"
                />
              <input class="sf-search sfLookup sf-local-rdf-link" type="text" name="clas" size="25"
                placeholder={ classMessage }
                title={ classMessage }
                data-rdf-type={fromUri(rdfs.Class)}
                data-rdf-property={fromUri(rdf.typ)}
                />
              <input class="hasLookup sf-local-rdf-link" type="text" name="link" size="25"
                placeholder={ dbpediaMessage }
                title={ "" }
                />
              { rdfStartingPoints }
            <input class="btn btn-primary" type="submit" value={ messageI18N("Search") }/>
        </form>
    <br/>
  }

  private def htmlSuggestedClassForCreation(uri: Rdf#URI, lang: String): NodeSeq = {
    // TODO use creationButton()
//    println( s"lang $lang , uri $uri")
    <span><a href={
      "/create?prefill=no&uri=" + encode(uri)
    }>
            { instanceLabelFromTDB(uri, lang) }
            ( { abbreviateTurtle(uri) } )
          </a> - </span>
  }

  private def encode(u: Rdf#URI): String = URLEncoder.encode(fromUri(u), "UTF-8")

  /** suggested Classes For Creation */
  private def suggestedClassesForCreation(lang: String): NodeSeq = {
    val suggestedClasses: Seq[Rdf#URI] = Seq(
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

    val resultTry = wrapInReadTransaction {
      val nodes = for (suggestedClass <- suggestedClasses) yield
        htmlSuggestedClassForCreation(suggestedClass, lang)
      val nf: NodeSeq = nodes.flatten
      nf
    }
    resultTry match {
      case Success(nodesSeq) => nodesSeq
      case Failure(f) =>
        lang match {
          case "fr" => <p>{ s"Erreur dans les liens de création: $f" }</p>
          case "en" => <p>{ s"Error in creation links: $f" }</p>
          case _    => <p>{ s"Error in creation links: $f" }</p>
        }
    }
  }

  /** compute HTML links to Classes For Creating Instance */
  private def computeEnterClassForCreatingInstance()(implicit lang: String = "en"): NodeSeq =

    <div class="row sf-margin-top-10">
      <div class="col-xs-12 col-sm-12 col-md-12 col-md-offset-1">
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
      </div>
    </div>

  private val suggestedClassesForCreationLang = scala.collection.mutable.Map[String, NodeSeq]()

  /** HTML links to Classes For Creating Instance
   *  Used in main page */
  def enterClassesForCreatingInstance(lang: String = "en"): NodeSeq = {
    val alreadyThere = suggestedClassesForCreationLang.keys.toList.contains(lang)
    if( alreadyThere )
      suggestedClassesForCreationLang.get(lang).get
    else {
      val ret = computeEnterClassForCreatingInstance()(lang)
      suggestedClassesForCreationLang.put(lang, ret)
      ret
    }
  }

}
