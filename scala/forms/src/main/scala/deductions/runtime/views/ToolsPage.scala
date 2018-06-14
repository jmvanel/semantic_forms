package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.html.BasicWidgets
import deductions.runtime.utils.{I18NMessages, URIManagement}
import deductions.runtime.core.HTTPrequest

import scala.xml.{NodeSeq, Unparsed}
import deductions.runtime.utils.Configuration

trait ToolsPage extends EnterButtons
    with BasicWidgets
    with URIManagement {

  val config: Configuration

  /** HTML page with 2 SPARQL Queries: select & construct, show Named Graphs, history, YASGUI, etc */
  def getPage(lang: String = "en", request: HTTPrequest ): NodeSeq = {
		def absoluteURI = request.absoluteURL()
		def localSparqlEndpoint = URLEncoder.encode(absoluteURI + "/sparql2", "UTF-8")

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
         |CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10
         |#CONSTRUCT { ?S geo:long ?LON ; geo:lat ?LAT ; rdfs:label ?LAB; foaf:depiction ?IMG.}
         |#WHERE {GRAPH ?GRAPH { ?S geo:long ?LON ; geo:lat ?LAT ; rdfs:label ?LAB. OPTIONAL{?S foaf:depiction ?IMG} OPTIONAL{?S foaf:img ?IMG} } }"""
    .stripMargin



    <link href="assets/images/favicon.png" type="image/png" rel="shortcut icon"/>
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
      <p>
        <a href={
          s"""http://yasgui.org?endpoint=$localSparqlEndpoint"""
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
      <p> <a href="..">{ I18NMessages.get("MainPage", lang) }</a> </p>
    </div>
  }

  /** HTML Form for a sparql Query, with execution buttons */
  def sparqlQueryForm( viewButtons: Boolean, query: String, action: String,
      sampleQueries: Seq[String], request: HTTPrequest): NodeSeq = {
    val textareaId = s"query-$action" . replaceAll("/", "-")
    println( "textareaId " + textareaId);

    val buttonsNextRelease = Seq(
      <input class="btn btn-primary" type="submit" value={ I18NMessages.get("View", request.getLanguage()) }
             formaction="/sparql-form"/>,
      makeLinkCarto( textareaId, config.geoMapURL, request ),
//          "http://rawgit.com/Cruis-R/geo-map-component/master/docs/index.html"),
      <input class="btn btn-primary" type="submit" value={ I18NMessages.get("Table", request.getLanguage()) }
             formaction="/table"/>,

      <input class="btn btn-primary" type="submit"
             title="NOT YET IMPLEMENTED"
             disabled="disabled"
             value={ I18NMessages.get("Tree", request.getLanguage() ) }/>,
      makeLink(textareaId, "/assets/rdfviewer/rdfviewer.html?url="))

    <form role="form">
      <textarea name="query" id={textareaId} style="min-width:80em; min-height:8em" title="To get started, uncomment one of these lines.">{
        if (query != "")
          query          
        else
          sampleQueries .mkString ("\n" )
      }</textarea>

      <div class="container">
        <div class="btn-group">
          <input class ="btn btn-primary" type="submit"
                 value={ I18NMessages.get("Submit", request.getLanguage()) } formaction ={action} />
          <label>unionDefaultGraph</label>
          <input name="unionDefaultGraph" type="checkbox"
                 checked={if (request.getHTTPparameterValue("unionDefaultGraph").isDefined) "yes" else null} />
          { if (viewButtons) buttonsNextRelease
          else
             <input class="btn btn-primary" type="submit" value={ I18NMessages.get("History", request.getLanguage()) }
             formaction="/history"/> }
        </div>
      </div>
    </form>
  }

  /** NOTE: for RDF Viewer this cannot work in localhost (because of rdfviewer limitations);
   *  works only on a hosted Internet server.
   *  TODO merge with function makeLinkCarto */
  private def makeLink(textareaId: String, toolURLprefix: String,
               toolname: String = "RDF Viewer",
               imgWidth: Int = 15): NodeSeq = {

    val sparqlServicePrefix = URLEncoder.encode( URLEncoder.encode("sparql?query=", "UTF-8"), "UTF-8")
    val buttonId = textareaId+"-button-1"
    val ( servicesURIPrefix, isDNS) = servicesURIPrefix2
    println(s"servicesURIPrefix $servicesURIPrefix, is DNS $isDNS")
    val servicesURIPrefixEncoded = URLEncoder.encode( URLEncoder.encode(servicesURIPrefix, "UTF-8"), "UTF-8")
    val servicesURL = s"$toolURLprefix$servicesURIPrefixEncoded$sparqlServicePrefix"
    println(s">>>> servicesURL $servicesURL")

    <button id={buttonId}
    class="btn btn-default" title={ s"Draw RDF graph diagram with $toolname" } target="_blank">
      <img width={ imgWidth.toString() } border="0" src="https://www.w3.org/RDF/icons/rdf_flyer.svg"
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
      '%0D%0Aurldecode';
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

    val dataServicesURL = sparqlServicesURL
    val buttonId = textareaId+"-button"

    <input id={buttonId} type="submit"
           title="make LeafLet map (OSM)"
           class="btn btn-primary" target="_blank"
           value={ I18NMessages.get("Map", request.getLanguage() )}>
    </input>
    <input class="btn btn-primary" type="checkbox" checked="true" title="points (else path)"
           id="points-path" />
    <script>
        { Unparsed( s"""
(function() {
  var textarea = document.getElementById('$textareaId');
  console.log('textareaId "$textareaId", textarea ' + textarea);
  var button = document.getElementById('$buttonId');

  var pointsCheckbox = document.getElementById('points-path');
  var pointsOrPath = pointsCheckbox.checked;
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
    console.log( 'data services URL= $dataServicesURL' );
    var url = '$toolURLprefix' +
      '?view=' + pointsOrPathValue +
      '&enrich=yes' +
      "&link-prefix=" + ${ s""""${request.host + config.hrefDisplayPrefix}""""} +
      "&lang=" + "${request.getLanguage()}" +
      '&url=' +
      '$dataServicesURL' + window.encodeURIComponent(query);
    console.log( 'URL= ' + url );
    window.open( url , '_blank' );
  });
}());
""")}
    </script>
  }
   
  private lazy val sparqlServicesURL = {
		val ( servicesURIPrefix, isDNS) = servicesURIPrefix2
    println(s"servicesURIPrefix $servicesURIPrefix, is DNS $isDNS")
    val sparqlServicePrefix = "sparql?query="
    val dataServicesURL = s"$servicesURIPrefix$sparqlServicePrefix"
    println(s">>>> dataServicesURL $dataServicesURL")
    dataServicesURL
  }
}

