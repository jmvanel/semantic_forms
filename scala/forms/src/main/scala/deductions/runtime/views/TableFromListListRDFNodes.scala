package deductions.runtime.views

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import scala.xml.NodeSeq

trait TableFromListListRDFNodes[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  def makeHtmlTable(nodesLists: List[Seq[Rdf#Node]]): NodeSeq = {
    <table> {
    for (row <- nodesLists) yield {
      <tr>{
        for (cell <- row) yield {
          <td>{ cell }</td>
        }
      }</tr>
    }
    }</table>
  }
}