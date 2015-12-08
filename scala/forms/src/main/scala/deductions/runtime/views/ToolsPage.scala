package deductions.runtime.views

import scala.xml.NodeSeq
import deductions.runtime.html.EnterButtons

trait ToolsPage extends EnterButtons {

  def getPage: NodeSeq = {
    <div>
      <p>
        SPARQL select
        {
          sparqlQueryForm("", "/select",
            "SELECT * WHERE {{ GRAPH ?G {{?S ?P ?O . }} }} LIMIT 10")
        }
      </p>
      <p>
        SPARQL construct
        {
          sparqlQueryForm("", "/sparql",
            "CONSTRUCT { ?S ?P ?O . } WHERE { GRAPH ?G { ?S ?P ?O . } } LIMIT 10")
        }
        <p>
          Graphs TODO
        </p>
        <p>
          Dashboard TODO
        </p>
        { enterURItoDownloadAndDisplay() }
        <p>
          <a href="..">Back to Main page</a>
        </p>
      </p>
    </div>
  }

  def sparqlQueryForm(query: String, action: String, sampleQuery: String): NodeSeq =
    <form role="form" action={ action }>
      query:
      <textarea name="query" cols="80">
        {
          if (query != "")
            query
          else
            "# " + sampleQuery
        }
      </textarea>
      <input type="submit" value="Submit"/>
    </form>
}