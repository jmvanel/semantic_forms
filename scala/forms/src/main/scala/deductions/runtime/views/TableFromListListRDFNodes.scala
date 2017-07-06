package deductions.runtime.views

import org.w3.banana.{RDF, RDFOps}

import scala.xml.NodeSeq

trait TableFromListListRDFNodes[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]

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