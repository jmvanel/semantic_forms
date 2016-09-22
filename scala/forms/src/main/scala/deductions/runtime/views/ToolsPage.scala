package deductions.runtime.views

import scala.xml.NodeSeq
import deductions.runtime.html.EnterButtons
import deductions.runtime.utils.I18NMessages

trait ToolsPage extends EnterButtons {

  def getPage(lang: String = "en"): NodeSeq = {
    <div>
      <p>
        SPARQL select
        {
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm("", "/select", Seq(
            "SELECT * WHERE { GRAPH ?G {?S ?P ?O . } } LIMIT 100",
            "SELECT DISTINCT ?CLASS WHERE { GRAPH ?G { [] a  ?CLASS . } } LIMIT 100",
            "SELECT DISTINCT ?PROP WHERE { GRAPH ?G { [] ?PROP [] . } } LIMIT 100"
          ))
        }
      </p>
      <p>
        SPARQL construct
        {
          // TODO: the URL here appears also in Play! route!
          sparqlQueryForm("", "/sparql-ui",
            Seq("CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10"))
        }
      </p>
      <p> <a href="/showNamedGraphs">{ I18NMessages.get("showNamedGraphs", lang) }</a> </p>
      <p> <a href="/history">{ I18NMessages.get("Dashboard", lang) }</a> </p>
      {
        implicit val lang1: String = lang
        enterURItoDownloadAndDisplay()
      }
      <p> <a href="..">{ I18NMessages.get("MainPage", lang) }</a> </p>
    </div>
  }

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