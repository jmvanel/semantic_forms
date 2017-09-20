package deductions.runtime.services

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.I18NMessages
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

import scala.xml.NodeSeq
import scala.xml.Elem

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

  implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = ""
    override def variables = Seq("SUBJECT", "TIME", "COUNT")
  }

  private def mess(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  /**
   * leverage on ParameterizedSPARQL.makeHyperlinkForURI()
   */
  def makeTableHistoryUserActions(request: HTTPrequest)(limit: String): NodeSeq = {
    val metadata = getMetadata()(limit)
    implicit val lang = request.getLanguage()
    val historyLink: Elem = {
      if (limit != "")
        <a href="/history">Complete history</a>
      else
        <div></div>
    }

    {
      Seq(
        historyLink,
        <table class="table">
          <thead>
            <tr>
              <th title="Resource URI visited by user">{ mess("Resource") }</th>
              <th title="Type">Type</th>
              <th title="Action (Create, Display, Update)">{ mess("Action") }</th>
              <th title="Time visited by user">{ mess("Time") }</th>
              <th title="Number of fields (triples) edited by user">{ mess("Count") }</th>
              <th>{ mess("User") }</th>
              <!--th>IP</th-->
            </tr>
          </thead>
          <tbody>
            {
              def dateAsLong(row: Seq[Rdf#Node]): Long = makeStringFromLiteral(row(1)).toLong

              val sorted = metadata.sortWith {
                (row1, row2) =>
                  dateAsLong(row1) >
                    dateAsLong(row2)
              }
              wrapInTransaction { // for calling instanceLabel()
                for (row <- sorted) yield {
                  logger.debug("row " + row(1).toString())
                  if (row(1).toString().length() > 3) {
                    val date = new Date(dateAsLong(row))
                    val dateFormat = new SimpleDateFormat(
                      "EEEE dd MMM yyyy, HH:mm", Locale.forLanguageTag(lang))
                    <tr>{
                      <td>{ makeHyperlinkForURI(row(0), lang, allNamedGraph, config.hrefDisplayPrefix) }</td>
                      <td>{
                        makeHyperlinkForURI(getClassOrNullURI(row(0))(allNamedGraph),
                          lang, allNamedGraph, config.hrefDisplayPrefix)
                      }</td>
                      <td>{ "Edit" /* TODO */ }</td>
                      <td>{ dateFormat.format(date) }</td>
                      <td>{ makeStringFromLiteral(row(2)) }</td>
                      <td>{ row(3) }</td>
                    }</tr>
                  } else <tr/>
                }
              }.get
            }
          </tbody>
        </table>)
    }
  }
}
