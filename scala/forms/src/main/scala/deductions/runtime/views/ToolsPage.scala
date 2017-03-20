package deductions.runtime.views

import scala.xml.NodeSeq
import java.net.URLEncoder

import deductions.runtime.utils.I18NMessages
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.html.BasicWidgets
import scala.xml.Unparsed
import deductions.runtime.services.URIManagement

trait ToolsPage extends EnterButtons
    with BasicWidgets
    with URIManagement {

  def getRequest(): HTTPrequest = HTTPrequest() // TODO <<<<<<<<<<< remove
  def absoluteURI = getRequest().absoluteURL("")
  def localSparqlEndpoint = URLEncoder.encode(absoluteURI + "/sparql", "UTF-8")

  /** HTML page with 2 SPARQL Queries: select & construct, show Named Graphs, history, YASGUI, etc */
  def getPage(lang: String = "en"): NodeSeq = {
    val querySample =
      """|PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |SELECT * WHERE { graph ?g {
      |  ?sub ?pred ?obj .
      |}} LIMIT 3""".stripMargin

    <link href="assets/images/favicon.png" type="image/png" rel="shortcut icon"/>
    <div>
      <p>
        SPARQL select {
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm(false, "", "/select-ui", Seq(
            "SELECT * WHERE { GRAPH ?G {?S ?P ?O . } } LIMIT 100",
            "SELECT DISTINCT ?CLASS WHERE { GRAPH ?G { [] a  ?CLASS . } } LIMIT 100",
            "SELECT DISTINCT ?PROP WHERE { GRAPH ?G { [] ?PROP [] . } } LIMIT 100"
          ))
        }

      </p>
      <p>
        SPARQL construct {
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm(true, "", "/sparql-ui",
            Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10"))
           
         }  
       

      </p>
      <p> <a href={
        s"""http://yasgui.org?endpoint=$localSparqlEndpoint,query=${URLEncoder.encode(querySample, "UTF-8")}"""
      } target="_blank">YasGUI</a> (Yet Another SPARL GUI) </p>
      <p> <a href="/showNamedGraphs">{ I18NMessages.get("showNamedGraphs", lang) }</a> </p>
      <p> <a href="/history">{ I18NMessages.get("Dashboard", lang) }</a> </p>
      {
        implicit val lang1: String = lang
        "Charger / Load" ++ enterURItoDownloadAndDisplay()
      }
      <p> <a href="..">{ I18NMessages.get("MainPage", lang) }</a> </p>
    </div>
  }

  /** HTML Form for a sparql Query, with execution buttons */
  def sparqlQueryForm(viewButton: Boolean, query: String, action: String, sampleQueries: Seq[String]): NodeSeq = {
    val textareaId = s"query-$action" . replaceAll("/", "-")
    println( "textareaId " + textareaId);
    <form role="form">
      <textarea name="query" id={textareaId} style="min-width:80em; min-height:8em" title="To get started, uncomment one of these lines.">{
        if (query != "")
          query          
        else
          for (sampleQuery <- sampleQueries) yield "# " + sampleQuery + "\n" 
      }</textarea>
      <input type="submit" value="Submit" formaction ={action}/>
			
			{	if (viewButton)
			  Seq(
      <input type="submit" value="View" formaction ="/sparql-form"/> ,
      <div>- </div> ,
        makeLink(textareaId, "/assets/rdfviewer/rdfviewer.html?url=" )
			)}
			</form>
  }

  /** NOTE: this cannot work in localhost (because of rdfviewer limitations);
   *  works only on a hosted Internet server. */
  def makeLink(textareaId: String, toolURLprefix: String,
               toolname: String = "RDF Viewer",
               imgWidth: Int = 15): NodeSeq = {

    val sparqlServicePrefix = "/sparlq?query="
    val buttonId = textareaId+"-button"
    println(s"servicesURIPrefix $servicesURIPrefix")
    val servicesURL = s"$toolURLprefix$servicesURIPrefix$sparqlServicePrefix"
    println(s"servicesURL $servicesURL")

    <button id={buttonId}
    class="btn btn-default" title={ s"Draw RDF graph with $toolname" } target="_blank">
      <img width={ imgWidth.toString() } border="0" src="https://www.w3.org/RDF/icons/rdf_flyer.svg"
      alt="RDF Resource Description Framework Flyer Icon"/>
    </button>
    <script>
{ Unparsed( s"""
(function() {
  var textarea = document.getElementById('$textareaId');
  console.log('textareaId "$textareaId", textarea ' + textarea);
  var button = document.getElementById('$buttonId');
  button.addEventListener( 'click', function() {
    console.log( 'elementById ' + textarea);
    var query = textarea.value;
    console.log( 'query in textarea ' + query);
    console.log( 'services URL $servicesURL' );
    var url = '$servicesURL' + window.encodeURIComponent(query) ;
    console.log( 'URL ' + url );
    // console.log( 'startsWith ' + ( '$sparqlServicePrefix' . startsWith ('/') ) );
    if( ! '$sparqlServicePrefix' . startsWith ('/') )
      window.open( url , '_blank' );
    else
      console.log( 'RDFViewer works only on a hosted Internet serverURL !!!' );
  });
}());
""")}
    </script>
  }
   
                        
}