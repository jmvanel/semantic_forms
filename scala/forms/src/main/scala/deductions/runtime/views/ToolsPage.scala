package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.html.BasicWidgets
import deductions.runtime.utils.{I18NMessages, URIManagement}
import deductions.runtime.core.HTTPrequest

import scala.xml.{NodeSeq, Unparsed}
import deductions.runtime.utils.Configuration
import org.w3.banana.RDF

trait ToolsPage[Rdf <: RDF, DATASET] extends EnterButtons[Rdf, DATASET]
    with BasicWidgets[Rdf#Node, Rdf#URI]
    with URIManagement {

  val config: Configuration

  /** HTML page with 2 SPARQL Queries: select & construct, show Named Graphs, history, YASGUI, etc */
  def getPage(request: HTTPrequest ): NodeSeq = {
		def absoluteURI = request.absoluteURL()
		def localSparqlEndpoint = URLEncoder.encode(absoluteURI + "/sparql2", "UTF-8")

    val lang = request.getLanguage
    val querySampleSelect =
    """|PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |SELECT * WHERE { GRAPH ?G {?S ?P ?O . } } LIMIT 100
       |
       |#SELECT DISTINCT ?CLASS WHERE { GRAPH ?G { [] a  ?CLASS . } } LIMIT 100
       |#SELECT DISTINCT ?PROP WHERE { GRAPH ?G { [] ?PROP [] . } } LIMIT 100
    """.stripMargin
    val querySampleConstruct =
      """|PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         |PREFIX spatial: <http://jena.apache.org/spatial#>
         |CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10
         |#CONSTRUCT { ?S geo:long ?LON ; geo:lat ?LAT ; rdfs:label ?LAB; foaf:depiction ?IMG.}
         |#WHERE {GRAPH ?GRAPH { ?S geo:long ?LON ; geo:lat ?LAT ; rdfs:label ?LAB. OPTIONAL{?S foaf:depiction ?IMG} OPTIONAL{?S foaf:img ?IMG} } }
         |#       ?S spatial:withinBox( 43.0 0.0 48.0 10.0  100 #km )"""
    .stripMargin

    <link href="/assets/images/favicon.png" type="image/png" rel="shortcut icon"/>
    <div>
      <p>
        SPARQL select {
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm( false, "", "/select-ui", Seq( querySampleSelect ), request)
        }

      </p>
      <p>
        SPARQL construct {
          sparqlQueryForm( true, "",
        		  // TODO: the URL here appears also in Play! route!
              "/sparql-ui",
            Seq( querySampleConstruct ), request)
         } 
      </p>
      <a href="https://jena.apache.org/documentation/geosparql/">Jena GeoSPARQL doc.</a>
      <p>
        <a href={
          s"""http://yasgui.triply.cc#?endpoint=$localSparqlEndpoint"""
        } target="_blank">
          YasGUI
        </a>
        (Yet Another SPARQL GUI)
      </p>

      <p>
        <a href={
          val sparklisServer =
            s"http://www.irisa.fr/LIS/ferre/sparklis/osparklis.html"
          s"""$sparklisServer?title=Semantic+forms&endpoint=$localSparqlEndpoint"""
        } target="_blank">
          Sparklis
        </a>
        (un requêteur SPARQL original et puissant)
      </p>

      <p>
        <a href={
          val server =
            s"http://tools.sirius-labs.no/rdfsurveyor"
          s"""$server?title=Semantic+forms&repo=$localSparqlEndpoint"""
        } target="_blank">
          SPARQL surveyor
        </a>
        (un autre requêteur SPARQL)
      </p>

      { showContinuationForm(request, Some("showNamedGraphs")) }
      <p> <a href="/history">{ I18NMessages.get("Dashboard", lang) }</a> </p>
      {
        implicit val lang1: String = lang
        "Charger / Load" ++ enterURItoDownloadAndDisplay()
      }
      { json2rdfForm(request) }
      { rdf2jsonForm(request) }
      <p>Free Memory {val mb = 1024 * 1024; Runtime.getRuntime.freeMemory().toFloat / mb} Mb</p>
      <p> <a href="..">{ I18NMessages.get("MainPage", lang) }</a> </p>
    </div>
  }

  /** HTML Form for a sparql Query, with execution buttons
   *  TODO : harmonize HTTP parameters sparql and sfserver */
  def sparqlQueryForm( viewButtonsForConstruct: Boolean, query: String, action: String,
      sampleQueries: Seq[String], request: HTTPrequest): NodeSeq = {
    val textareaId = s"query-$action" . replaceAll("/", "-")
//    println( "textareaId " + textareaId);

    val buttonsAllRDFViews = Seq(
      <input title="Plain form view"
             class="btn btn-primary" type="submit" value={ I18NMessages.get("View", request.getLanguage) }
             formaction="/sparql-form"/>,
      makeLinkCarto( textareaId, config.geoMapURL, request ),
      makeLinkToVisualTool(textareaId,
          "/assets/rdf-calendar/rdf-calendar.html?" +
          "sfserver=" + URLEncoder.encode(servicesURIPrefix, "UTF-8") +
          "&url=",
          "Calendar", 15,
          "/assets/images/calendar-tool-for-time-organization.svg",
          request=request),
      <input title="Table view"
             class="btn btn-primary" type="submit" value={ I18NMessages.get("Table", request.getLanguage) }
             formaction="/table"/>,
      {paragraphsViewInput(request)},
      {formOnlyViewInput(request)},
      {orderbyViewInput(request)},
      {detailsViewInput(request)},
      {labelViewInput(request)},

      <input title="Tree view - NOT YET IMPLEMENTED"
             class="btn btn-primary" type="submit"
             disabled="disabled"
             value={ I18NMessages.get("Tree", request.getLanguage ) }/>,
      makeLinkToVisualTool(textareaId,
          "/assets/rdfviewer/rdfviewer.html?url=",
          "RDF_Viewer", 15,
          request=request),
      makeLinkToVisualTool(textareaId,
          spoggyToolURL,
          // "sparql=" + URLEncoder.encode(servicesURIPrefix, "UTF-8") +
//          "&url=",
          "Spoggy", 15,
          request=request)
      )

    <form role="form">
      <textarea name="query" id={textareaId} style="min-width:80em; min-height:8em; font-family: courier" title="To get started, uncomment one of these lines.">{
        if (query != "")
          query          
        else
          sampleQueries .mkString ("\n" )
      }</textarea>

      <div class="container">
        <div class="btn-group">

          <!-- Buttons common to CONSTRUCT and SELECT -->
          <input class ="btn btn-primary" type="submit"
                 value={ I18NMessages.get("Submit", request.getLanguage) } formaction ={action} />

          <label title="RDF names graphs are processed as if they were the root (unnamed) graph">&nbsp; unionDefaultGraph</label>
          <input name="unionDefaultGraph" id="unionDefaultGraph" type="checkbox"
                 checked={ request.getHTTPparameterValue("unionDefaultGraph").getOrElse(null) } />

          { if (viewButtonsForConstruct)
            buttonsAllRDFViews
          else
             <input class="btn btn-primary" type="submit" value={
                 I18NMessages.get("History", request.getLanguage) }
               formaction="/history"
               title="Chronological view (only local edits)"/> ++
             {paragraphsViewInput(request)} ++
             {formOnlyViewInput(request)} ++
             <label title="Takes first URI in each response row and loads it in RDF database">&nbsp; load URI's</label> ++
             <input name="load-uris" id="load-uris" type="checkbox" /> ++
             <label title="compute Labels for URI's">&nbsp; compute Labels</label> ++
             <input name="compute-Labels" id="compute-Labels" type="checkbox" />
          }
          { htmlSelectForRDFmime }
        </div>
      </div>
    </form>
  }

  def paragraphsViewInput(request: HTTPrequest) =
    <input class="btn btn-primary" type="checkbox" title="paragraphs view (else table)"
      id="paragraphs" name="paragraphs"
      checked={ request.getHTTPparameterValue("paragraphs").getOrElse(null) } />
  // layout=form
  def formOnlyViewInput(request: HTTPrequest) =
    <input class="btn btn-primary" type="checkbox" title="form only (to include in any HTML page)"
      id="formOnly" name="layout"
      checked={ request.getHTTPparameterValue("formOnly").getOrElse(null) } />
  def orderbyViewInput(request: HTTPrequest) =
    <input class="" title="Order by given property"
      id="orderby" name="orderby"
      value={ request.getHTTPparameterValue("orderby").getOrElse(null) } />
  def detailsViewInput(request: HTTPrequest) =
    <input class="" title="Details level (default full details)"
      id="details" name="details"
      value={ request.getHTTPparameterValue("details").getOrElse(null) } />
  def labelViewInput(request: HTTPrequest) =
    <input class="" title="A title for the query and its views"
      id="label" name="label"
      value={ request.getHTTPparameterValue("label").getOrElse(null) } />

  /** make Link to visualization Tool, Graph (diagram) or other kind.
   *  NOTE: for RDF Viewer this cannot work in localhost (because of rdfviewer limitations);
   *  works only on a hosted Internet server.
   *  TODO
   *  - probably no need of this complex JavaScript just to send a form! Should as simple as action "/table".
   *  - merge with function makeLinkCarto */
  private def makeLinkToVisualTool(textareaId: String, toolURLprefix: String,
               toolname: String,
               imgWidth: Int,
               imgURL: String="https://www.w3.org/RDF/icons/rdf_flyer.svg",
               request: HTTPrequest): NodeSeq = {

    val sparqlServicePrefixEncoded = URLEncoder.encode("sparql?query=", "UTF-8")
    val ( servicesURIPrefix, isDNS) = servicesURIPrefix2
    logger.debug(s"servicesURIPrefix $servicesURIPrefix, is DNS $isDNS")
    val servicesURIPrefixEncoded = URLEncoder.encode(servicesURIPrefix, "UTF-8")
    val servicesURL = s"$toolURLprefix$servicesURIPrefixEncoded$sparqlServicePrefixEncoded"
    logger.debug(s">>>> makeLinkToVisualTool: servicesURL $servicesURL")

    val buttonId = textareaId+"-button-" + toolname
    <button id={buttonId}
    class="btn btn-default" title={ s"Draw RDF with $toolname" } target="_blank">
      <img width={ imgWidth.toString() } border="0" src={imgURL}
      alt="RDF Resource Description Framework Flyer Icon"/>
    </button>
    <script>
{ Unparsed( s"""
(function() {
  var textarea = document.getElementById('$textareaId');
  console.log('textareaId "$textareaId", textarea ' + textarea);
  var button = document.getElementById('$buttonId');
  console.log('button ' + button);
  button.addEventListener( 'click', function() {
    console.log( 'elementById ' + textarea);
    var query = textarea.value;
    console.log( 'query in textarea ' + query);
    console.log( 'services URL $servicesURL' );
    var url = '$servicesURL' +
      window.encodeURIComponent( window.encodeURIComponent(query)) +
      "&label=" + document.getElementById('label').value
    console.log( 'URL ' + url );
    if($isDNS)
      window.open( url , '_blank' );
    else {
      var message = "RDFViewer works only on a hosted Internet serverURL !!! URL is " + url;
      console.log( message );
      alert(message);
    }
  });
    console.log('After button.addEventListener');
}());
""")}
    </script>
  }

    /** make Link for (geographic) Cartography */
  private def makeLinkCarto(textareaId: String, toolURLprefix: String,
      request: HTTPrequest): NodeSeq = {

    val buttonId = textareaId+"-button"

    <input id={buttonId} type="submit"
           title="make LeafLet map (OSM)"
           class="btn btn-primary" target="_blank"
           value={ I18NMessages.get("Map", request.getLanguage )}>
    </input>
    <input class="btn btn-primary" type="checkbox" checked="true" title="points (else path)"
           id="points-path" />
    <script>
        { Unparsed( s"""
(function() {
  var textarea = document.getElementById('$textareaId');
  var unionDefaultGraph = document.getElementById('unionDefaultGraph').checked;

  console.log('textareaId "$textareaId", textarea ' + textarea);
  var button = document.getElementById('$buttonId');

  var pointsCheckbox = document.getElementById('points-path');
  var pointsOrPath = pointsCheckbox.checked;
  var dataServicesURL = '${sparqlServicesURL("", request)}'
  if (unionDefaultGraph)
    dataServicesURL = '${sparqlServicesURL("2", request)}'
  console.log('pointsOrPath ' + pointsOrPath);
  if( pointsOrPath )
    var pointsOrPathValue = 'points';
  else
    var pointsOrPathValue = 'path';
  console.log('pointsOrPathValue ' + pointsOrPathValue);

  button.addEventListener( 'click', function() {
    console.log( 'elementById ' + textarea);
    var query = textarea.value;
    console.log( 'query in textarea ' + query);
    console.log( 'data services URL= ' + dataServicesURL );
    var url = '$toolURLprefix' +
      '?view=' + pointsOrPathValue +
      '&enrich=yes' +
      "&link-prefix=" + ${ s""""${request.absoluteURL() + config.hrefDisplayPrefix}""""} +
      "&lang=" + "${request.getLanguage}" +
      // "&label=" + ${request.getHTTPparameterValue("label").getOrElse("")} +
      "&label=" + document.getElementById('label').value +
      '&url=' +
        dataServicesURL + window.encodeURIComponent(query);
    console.log( 'URL= ' + url );
    window.open( url , '_blank' );
  });
}());
""")}
    </script>
  }

  private def json2rdfForm(request: HTTPrequest): NodeSeq = {
    <form role="form" style="border: solid;">
      <fieldset>
        <p>JSON(-LD) to RDF</p>
        <div>JSON source</div>
        <input name="src" type="url" size="90" />
        <div>JSON-LD Expand @context</div>
        <input name="context" id="context" type="url" size="90" />
        <br/>
        <input class="btn btn-primary" type="submit" value={ I18NMessages.get("Submit", request.getLanguage) } formaction="/json2rdf"/>
      </fieldset>
    </form>
  }

  private def rdf2jsonForm(request: HTTPrequest): NodeSeq = {
    <form role="form" style="border: solid;">
      <fieldset>
        <p>RDF to JSON</p>
        <div>RDF source</div>
        <input name="src" type="url" size="90" />
        <div>JSON-LD frame @context</div>
        <input name="frame" type="url" size="90" />
        <br/>
        <input class="btn btn-primary" type="submit" value={ I18NMessages.get("Submit", request.getLanguage) }
        formaction="/rdf2json"/>
      </fieldset>
    </form>
  }

  /** TODO also in ApplicationFacadeImpl */
  private def sparqlServicesURL( suffix: String = "",
      request: HTTPrequest ) = {
    val servicesURIPrefix = request.absoluteURL()
    val sparqlServicePrefix = s"/sparql$suffix?query="
    val dataServicesURL = s"$servicesURIPrefix$sparqlServicePrefix"
    logger.debug(s">>>> lazy val dataServicesURL $dataServicesURL")
    dataServicesURL
  }
}

