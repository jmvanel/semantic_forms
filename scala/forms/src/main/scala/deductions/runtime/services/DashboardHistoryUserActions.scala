package deductions.runtime.services

import java.text.SimpleDateFormat
import java.util.{ Date, Locale }

import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.I18NMessages
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

import scala.xml.NodeSeq
import scala.xml.Elem
import deductions.runtime.views.ResultsDisplay

import scalaz._
import Scalaz._

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
  with ParameterizedSPARQL[Rdf, DATASET]
  with NavigationSPARQLBase[Rdf]
  with ResultsDisplay[Rdf, DATASET] {

  import ops._

  implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = ""
    override def variables = Seq("SUBJECT", "TIME", "COUNT")
  }

  private def mess(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  /**
   * leverage on ParameterizedSPARQL.makeHyperlinkForURI()
   */
  def makeTableHistoryUserActions(request: HTTPrequest)(limit: String): NodeSeq = {
    val metadata0 = getMetadata()(limit)
    val metadata1 = filterMetadata(metadata0, request)
    val metadata = filterMetadataFocus(metadata1, request)
    implicit val lang = request.getLanguage()
    val historyLink: Elem = {
      if (limit != "")
        <a href="/history">Complete history</a>
      else
        <div></div>
    }

    {
      val title: NodeSeq = {
        metadata._2 match {
          case Some(uri) =>
            println(s"==== makeTableHistoryUserActions: title: uri <$uri>")
            val focusPrettyPrinted = makeHyperlinkForURItr(URI(uri), lang, allNamedGraph)
            <div>{mess("View_centered")} </div> ++ focusPrettyPrinted
          case None => <div/>
        }
      }
      val html: NodeSeq =
        title ++
        historyLink ++
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

              val sorted = metadata._1.sortWith {
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
                        makeHyperlinkForURI(
                          getClassOrNullURI(row(0))(allNamedGraph),
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
        </table>
        html    
    }
  }

  /**
   * filter Metadata according to HTTP request, eg
   *  rdf:type=foaf:Person
   *
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user; ordered by recent first;
   */
  private def filterMetadata(
    metadata: List[Seq[Rdf#Node]],
    request:  HTTPrequest): List[Seq[Rdf#Node]] = {
    val params = request.queryString
    if (params.size > 1 ||
      (params.size === 1 &&
        params.head._1 =/= "limit" &&
        params.head._1 =/= "uri" &&
        params.head._1 =/= "query")) {

      wrapInReadTransaction {
        var filteredURIs = metadata
        for ((param0, values) <- params) {
          filteredURIs = filterOneCriterium(param0, values, filteredURIs)
        }
        filteredURIs
      }.getOrElse(metadata)
    } else
      metadata
  }

  /**  @param metadata List of Seq'q with subject, timestamp, triple count, user; ordered by recent first; */
  private def filterOneCriterium(
    param0: String, values: Seq[String],
    metadata: List[Seq[Rdf#Node]]): List[Seq[Rdf#Node]] = {
    var filteredURIs = metadata
    if (param0 != "limit") {
      val param = expandOrUnchanged(param0)
      for (value0 <- values) {
        val value = expandOrUnchanged(value0)
        println(s"filterMetadata: actually filter for param <$param> = <$value>")
        filteredURIs = filteredURIs.filter {
          row =>
            val uri = row(0)
            !find(
              allNamedGraph,
              uri, URI(param), URI(value)).isEmpty
        }
      }
    }
    filteredURIs
  }

  /**
   * filter Metadata with focus on an URI, according to HTTP request, eg
   *  uri=http://site.com
   */
  private def filterMetadataFocus(
    metadata:    List[Seq[Rdf#Node]],
    request:     HTTPrequest,
    querySPARQL: String              = ""): (List[Seq[Rdf#Node]], Option[String]) = {

    val params = request.queryString
    if (params.contains("uri")) {
      val focusURI = expandOrUnchanged( params("uri").headOption.getOrElse("") )
      println(s"""===== filterMetadataFocus: params.contains("uri") ${focusURI}""")
      val sparql = neighborhoodSearchSPARQL( focusURI )
      (filterMetadataSPARQL(
        metadata, request, sparql), Some(focusURI))
    } else (metadata, None)
  }

  /**
   * filter Metadata according to HTTP request, eg
   *  query=CONSTRUCT ...
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user; ordered by recent first
   */
  private def filterMetadataSPARQL(
    metadata: List[Seq[Rdf#Node]],
    request:  HTTPrequest, querySPARQL: String = ""): List[Seq[Rdf#Node]] = {
    println (s"""===== filterMetadataSPARQL: querySPARQL $querySPARQL""")

    val results = sparqlSelectQuery(querySPARQL)
//    println (s"""===== filterMetadataSPARQL: results $results """)

    /* merge URI's from query with metadata:
     * filter metadata with URI's in result */
    val uris = results.get.map {
      l => l.headOption.getOrElse(nullURI)
    }.toSet

    println (s"""===== filterMetadataSPARQL: uris $uris""")

    metadata.filter {
      row =>
        val uri = row(0)
        println (s"""== uri $uri""")
        uris . contains(uri)
    }
  }
}
