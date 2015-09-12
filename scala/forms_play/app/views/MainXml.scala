package views

import deductions.runtime.utils.I18NMessages
import controllers._
import scala.xml.NodeSeq
import deductions.runtime.user.RegisterPage
import org.w3.banana.RDF

trait MainXml // [Rdf <: RDF, DATASET] extends RegisterPage[Rdf, DATASET]
{

  def mainPage(content: NodeSeq, userInfo: NodeSeq, lang: String = "en") = {
    <html>
      { head(lang) }
      <body>
        {
          Seq(
            userInfo,
            mainPageHeader(lang),
            content)
        }
      </body>
    </html>
  }

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  def mainPageHeader(lang: String = "en"): NodeSeq = {
    def message(key: String) = I18NMessages.get(key, lang)
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
                  <option label="av:Person"> http://www.assemblee-virtuelle.org/ontologies/v1.owl#Person </option>
                  <option label="av:Organization"> http://www.assemblee-virtuelle.org/ontologies/v1.owl#Organization </option>
                  <option label="av:Project" title="Projet dans ontologie de l'Assemblée Virtuelle">
                    http://www.assemblee-virtuelle.org/ontologies/v1.owl#Project
                  </option>
                  <option label="av:Idea"> http://www.assemblee-virtuelle.org/ontologies/v1.owl#Idea </option>
                  <option label="av:Resource"> http://www.assemblee-virtuelle.org/ontologies/v1.owl#Resource </option>
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

  def head(lang: String = "en") = {
    def message(key: String) = I18NMessages.get(key, lang)
    <head>
      <title>{ message("Welcome") }</title>
      <meta http-equiv="Content-type" content="text/html; charset=UTF-8"></meta>
      <link rel="shortcut icon" type="image/png" href={ routes.Assets.at("images/favicon.png").url }/>
      <script src={ routes.Assets.at("javascripts/jquery-1.11.2.min.js").url } type="text/javascript"></script>
      <!-- Latest compiled and minified CSS -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
      <link rel="stylesheet" href={ routes.Assets.at("stylesheets/select2.css").url }/>
      <!-- Optional theme -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css"/>
      <!-- Latest compiled and minified JavaScript -->
      <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
      <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/css/select2.min.css" rel="stylesheet"/>
      <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-beta.3/js/select2.min.js"></script>
      <script src={ routes.Assets.at("javascripts/select2.js").url } type="text/javascript"></script>
      <script src={ routes.Assets.at("javascripts/wikipedia.js").url } type="text/javascript"></script>
      <script src={ routes.Assets.at("javascripts/formInteractions.js").url } type="text/javascript"></script>
      <style type="text/css">
        .resize {{ resize: both; width: 100%; height: 100%; }}
        .overflow {{ overflow: auto; width: 100%; height: 100%; }}
      </style>
    </head>
  }
}