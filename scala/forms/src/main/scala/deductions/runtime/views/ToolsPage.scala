package deductions.runtime.views

import scala.xml.NodeSeq
import deductions.runtime.html.EnterButtons
import deductions.runtime.utils.I18NMessages
import deductions.runtime.utils.HTTPrequest

trait ToolsPage extends EnterButtons {

  def getRequest(): HTTPrequest = HTTPrequest()
  def absoluteURI = getRequest().absoluteURL("")

  /** HTML page with 2 SPARQL Queries: select & construct, show Named Graphs, history, YASGUI, etc */
  def getPage(lang: String = "en"): NodeSeq = {
    val querySample =
      """|PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |SELECT * WHERE { graph ?g {
      |  ?sub ?pred ?obj .
      |}} LIMIT 5""".stripMargin

    <link href="assets/images/favicon.png" type="image/png" rel="shortcut icon"/>
    <div>
      <p>
        SPARQL select{
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm("", "/select-ui", Seq(
            "SELECT * WHERE { GRAPH ?G {?S ?P ?O . } } LIMIT 100",
            "SELECT DISTINCT ?CLASS WHERE { GRAPH ?G { [] a  ?CLASS . } } LIMIT 100",
            "SELECT DISTINCT ?PROP WHERE { GRAPH ?G { [] ?PROP [] . } } LIMIT 100"
          ))
        }
      </p>
      <p>
        SPARQL construct{
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm("", "/sparql-ui",
            Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10"))
        }
      </p>
      <p> <a href="/showNamedGraphs">{ I18NMessages.get("showNamedGraphs", lang) }</a> </p>
      <p> <a href="/history">{ I18NMessages.get("Dashboard", lang) }</a> </p>
      {
        implicit val lang1: String = lang
        "Charger / Load" ++ enterURItoDownloadAndDisplay()
      }
      <p> <a href={
        s"""http://yasgui.org?endpoint=$absoluteURI/sparql,
        query=$querySample"""
      } target="_blank">YasGUI</a> (Yet Another SPARL GUI) </p>
      <p> <a href="..">{ I18NMessages.get("MainPage", lang) }</a> </p>
    </div>
  }

  /** HTML Form for a sparql Query */
  def sparqlQueryForm(query: String, action: String, sampleQueries: Seq[String]): NodeSeq =
    <form role="form" action={ action }>
      <textarea name="query" style="min-width:80em; min-height:8em" title="To get started, uncomment one of these lines.">{
        if (query != "")
          query
        else
          for (sampleQuery <- sampleQueries) yield "# " + sampleQuery + "\n"
      }</textarea>
      <input type="submit" value="Submit"/>
    </form>
}