package deductions.runtime.views

import scala.xml.NodeSeq
import deductions.runtime.html.EnterButtons

trait ToolsPage extends EnterButtons {

  def getPage: NodeSeq = {
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
      <p> <a href="/showNamedGraphs">show Named Graphs</a> </p>
      <p> <a href="/history">Dashboard: history of user actions</a> </p>
      { enterURItoDownloadAndDisplay() }
      <p> <a href="..">Back to Main page</a> </p>
    </div>
  }

  def sparqlQueryForm(query: String, action: String, sampleQueries: Seq[String]): NodeSeq =
    <form role="form" action={ action }>
      <textarea name="query" cols="80" rows="4" title="To get started, uncomment one of these lines.">{
        if (query != "")
          query
        else
          for (sampleQuery <- sampleQueries) yield "# " + sampleQuery + "\n"
      }</textarea>
      <input type="submit" value="Submit"/>
    </form>
}