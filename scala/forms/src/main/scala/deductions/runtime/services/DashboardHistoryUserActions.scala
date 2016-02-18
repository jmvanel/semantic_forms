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
    with ParameterizedSPARQL[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._

  val qm = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String): String = ""
    override def variables = Seq("SUBJECT", "TIME", "COUNT")
  }

  /** leverage on ParameterizedSPARQL.makeHyperlinkForURI() */
  def makeTableHistoryUserActions(lang: String = "en")(implicit userURI: String): NodeSeq = {
    val met = getMetadata()
    implicit val queryMaker = qm
     <table class="table">
      <thead>
        <tr>
          <th title="Resource URI visited by user">Resource</th>
          <th title="Time visited by user">Time</th>
          <th title="Number of fields (triples) edited by user">Count</th>
          <th>User</th>
        </tr>
      </thead><tbody>
                {
                  dataset.rw({ // for calling instanceLabel()
                    for (row <- met) yield {
                      println("row " + row(1).toString())
                      if (row(1).toString().length() > 3) {
                        val date = new Date(makeStringFromLiteral(row(1)).toLong)
                        val dateFormat = new SimpleDateFormat(
                          "EEEE dd MMM yyyy, HH:mm", Locale.forLanguageTag(lang))
                        <tr>{
                          <td>{ makeHyperlinkForURI(row(0), lang, allNamedGraph) }</td>
                          <td>{ dateFormat.format(date) }</td>
                          <td>{ makeStringFromLiteral(row(2)) }</td>
                          <td>{ userURI }</td>
                        }</tr>
                      } else <tr/>
                    }
                  }).get
                }
    		</tbody></table>
  }
}