package deductions.runtime.services

import scala.concurrent.Future
import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import deductions.runtime.dataset.RDFStoreLocalProvider
import scala.xml.NodeSeq
import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Show History of User Actions:
 *  - URI
 *  - type of action: created, displayed, modified;
 *  - user,
 *  - timestamp,
 *  cf https://github.com/jmvanel/semantic_forms/issues/8
 */
trait DashboardHistoryUserActions[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with TimeSeries[Rdf, DATASET]
with InstanceLabelsInferenceMemory[Rdf, DATASET]
    {

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._

  /** leverage on Form2HTMLDisplay.createHTMLResourceReadonlyField() */
  def makeTableHistoryUserActions(lang: String="en")(implicit userURI: String): NodeSeq = {
    val met = getMetadata()
    <table>
      <tr><th>Resource</th> <th>Time</th> <th>Count</th> <th>User</th></tr>
      {
      for (row <- met) yield {
        val date = new Date( makeStringFromLiteral(row(1)).toLong )
        val dateFormat = new SimpleDateFormat(
            "EEEE dd MMM yyyy, HH:mm", Locale.forLanguageTag(lang))
        <tr>{
          <td>{row(0)}</td>
          <td>{ dateFormat.format(date) }</td>
          <td>{makeStringFromLiteral(row(2))}</td>
         <td>{ userURI }</td>
          // for (cell <- row) yield { <td>{cell}</td> }
        }</tr>
      }
    }
    </table>
  }
}