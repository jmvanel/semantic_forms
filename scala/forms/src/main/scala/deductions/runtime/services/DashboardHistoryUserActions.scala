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

  /** make Table of History of User Actions;
   * leverage on ParameterizedSPARQL.makeHyperlinkForURI();
   * available filters in HTML parameters:
   * - triple pattern, eg rdf:type=foaf:Person
   * - centered in given URI, eg uri=http://site.com ,
   * - filter according to a SPARQL query , given by sparql= , eg
   *   http://localhost:9000/history?sparql=SELECT DISTINCT ?thing WHERE {GRAPH ?G {?thing a <http://xmlns.com/foaf/0.1/Person> . }}
   */
  def makeTableHistoryUserActions(request: HTTPrequest)(limit: String): NodeSeq = {
    val metadata0 = getMetadata()(
//        if( request.getHTTPparameterValue("sparql").isDefined )
//          ""
//        else
        limit)
    logger.debug(s">>>> makeTableHistoryUserActions limit '$limit' metadata0 ${metadata0.size} $metadata0")
    val metadata1 = filterMetadata(metadata0, request)
    logger.debug(s">>>> makeTableHistoryUserActions metadata1 $metadata1")
    val metadata = filterMetadataFocus(metadata1, request)
    logger.debug(s">>>> makeTableHistoryUserActions metadata $metadata")
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
            logger.debug(s"==== makeTableHistoryUserActions: title: uri <$uri>")
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
              <th title="Type (class)">Type</th>
              <!-- th title="Action (Create, Display, Update)">{ mess("Action") }</th-->
              <th title="Time visited by user">{ mess("Time") }</th>
              <th>{ mess("User") }</th>
              <th title="Number of fields (triples) edited by user">{ mess("Count") }</th>
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
                  try {
                  logger.debug("row " + row(1).toString())
                  if (row(1).toString().length() > 3) {
                    val date = new Date(dateAsLong(row))
                    val dateFormat = new SimpleDateFormat(
                      "EEEE dd MMM yyyy, HH:mm", Locale.forLanguageTag(lang))
                    <tr class="sf-table-row">{
                      <td>{ makeHyperlinkForURI(row(0), lang, allNamedGraph, config.hrefDisplayPrefix) }</td>
                      <td>{
                        makeHyperlinkForURI(
                          getClassOrNullURI(row(0))(allNamedGraph),
                          lang, allNamedGraph, config.hrefDisplayPrefix)
                      }</td>
                      <!-- td>{ "Edit" /* TODO */ }</td -->
                      <td>{ dateFormat.format(date) }</td>
                      <td>{ row(3) }</td>
                      <td>{ makeStringFromLiteral(row(2)) }</td>
                    }</tr>
                  } else <tr/>
                  }
                  catch {
                    case t: Throwable => t.printStackTrace()
                    <tr>{ t.getLocalizedMessage }</tr>
                  }
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
        params.head._1 =/= "query" &&
        params.head._1 =/= "sparql") ) {

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

  /** filter given List according to One Criterium on first column:
   *  ?URI <param> <value> .
   *  @param param0 abbreviated Turtle URI for triple predicate
   *  @param values abbreviated Turtle URI for triple object
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user;
   *         ordered by recent first; */
  private def filterOneCriterium(
    param0: String, values: Seq[String],
    metadata: List[Seq[Rdf#Node]]): List[Seq[Rdf#Node]] = {
    var filteredURIs = metadata
    if (param0 != "limit") {
      val param = expandOrUnchanged(param0)
      for (value0 <- values) {
        val value = expandOrUnchanged(value0)
        logger.debug(s"filterMetadata: actually filter for param <$param> = <$value>")
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
   *  uri=http://site.com ,
   * or filter Metadata according to a SPARQL query , given by sparql= HTTP parameter
   */
  private def filterMetadataFocus(
    metadata:    List[Seq[Rdf#Node]],
    request:     HTTPrequest,
    querySPARQL: String              = ""): (List[Seq[Rdf#Node]], Option[String]) = {

    val params = request.queryString
    if (params.contains("uri")) {
      val focusURI = expandOrUnchanged( params("uri").headOption.getOrElse("") )
      logger.debug(s"""===== filterMetadataFocus: params.contains("uri") ${focusURI}""")
      val sparqlQuery = neighborhoodSearchSPARQL( focusURI )
      (filterMetadataSPARQL(
        metadata, request, sparqlQuery), Some(focusURI))

    } else if( params.contains("sparql") ||
               params.contains("query")) {
      logger.debug(s"""===== filterMetadataFocus: params.contains("sparql") or "query" """)
      val sparqlQuery = params.getOrElse("sparql",
                        params.getOrElse("query", Seq() )
                        ) .headOption.getOrElse("")
          params("query")
      println(s"filterMetadataFocus sparqlQuery $sparqlQuery")
      (filterMetadataSPARQL( metadata, request, sparqlQuery),
        Some("/user-query"))

    } else
      (metadata, None)
  }

  /**
   * filter Metadata according to HTTP request, eg
   *  query=CONSTRUCT ...
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user; ordered by recent first
   */
  private def filterMetadataSPARQL(
    metadata: List[Seq[Rdf#Node]],
    request:  HTTPrequest, querySPARQL: String = ""): List[Seq[Rdf#Node]] = {
    logger.debug(s"""===== filterMetadataSPARQL: querySPARQL $querySPARQL""")

    val results = sparqlSelectQuery(querySPARQL)
//    logger.trace(s"""===== filterMetadataSPARQL: results $results """)

    /* merge URI's from query with metadata:
     * filter metadata with URI's in result */
    val uris = results.get.map {
      l => l.headOption.getOrElse(nullURI)
    }.toSet

    logger.trace(
        s"""===== filterMetadataSPARQL: sparql Select result: uris $uris""")
    logger.debug(s"===== filterMetadataSPARQL: metadata.size ${metadata.size}")
    metadata.filter {
      row =>
        val uri = row(0)
        logger.debug(s"""== uri $uri""")
        uris . contains(uri)
    }
  }
}
