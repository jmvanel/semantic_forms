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
import deductions.runtime.services.html.TriplesViewWithTitle
import deductions.runtime.html.Form2HTML
import deductions.runtime.services.html.HTML5TypesTrait

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
  with TriplesViewWithTitle[Rdf, DATASET]
  with Form2HTML[Rdf#Node, Rdf#URI]
  with HTML5TypesTrait[Rdf] {

  import ops._

  implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = ""
    override def variables = Seq("SUBJECT", "TIME", "COUNT")
  }

  //  private def mess(key: String)(implicit lang: String) = I18NMessages.get(key, lang)

  /**
   * make Table of History of User Actions;
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
      val title: NodeSeq =
        <span>{ mess("View_centered") } </span> ++ metadata._2

      def dateAsLong(row: Seq[Rdf#Node]): Long = makeStringFromLiteral(row(1)).toLong
      val sorted = metadata._1.sortWith {
        (row1, row2) =>
          dateAsLong(row1) >
            dateAsLong(row2)
      }
      lazy val table =
        <table class="table">
          <thead>
            <tr style="color: LightGray; font-size: small">
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
              val dateFormat = new SimpleDateFormat(
                "dd MMM yyyy, HH:mm", Locale.forLanguageTag(lang))
              val dateFormat2 = new SimpleDateFormat("EEEE", Locale.forLanguageTag(lang))

              wrapInTransaction { // for calling instanceLabel()
                for (row <- sorted) yield {
                  try {
                    logger.debug("row " + row(1).toString())
                    val subjectURI = row(0)
                    val classOrNullURI = getClassOrNullURI(subjectURI)(allNamedGraph)
                    if (row(1).toString().length() > 3 &&
                      subjectURI != nullURI //                      classOrNullURI  != nullURI
                      ) {
                      val date = new Date(dateAsLong(row))
                      <tr class="sf-table-row">
                        <td>{
                          makeHyperlinkForURI(
                            subjectURI, allNamedGraph, config.hrefDisplayPrefix,
                            request = request)
                        }</td>
                        <td>{
                          makeHyperlinkForURI(
                            classOrNullURI, allNamedGraph, config.hrefDisplayPrefix,
                            request = request)
                        }</td>
                        <!-- td>{ "Edit" /* TODO */ }</td -->
                        <td title={ dateFormat2.format(date) }>{ dateFormat.format(date) }</td>
                        <td>{ row(3) /* user */ }</td>
                        <td>{ makeStringFromLiteral(row(2)) /* triple count */ }</td>
                      </tr>
                    } else <tr/>
                  } catch {
                    case t: Throwable =>
                      t.printStackTrace()
                      <tr>{ t.getLocalizedMessage }</tr>
                  }
                }
              }.get
            }
          </tbody>
        </table>

      lazy val paragraphs = paragrahsView(sorted, request)

      val html: NodeSeq =
        title ++
          historyLink ++ (
            if (request.getHTTPparameterValue("paragraphs").isDefined)
              paragraphs
            else table)
      html
    }
  }

  /**
   * filter Metadata according to HTTP request,
   * with a property-value pattern, eg
   *  rdf:type=foaf:Person
   *
   *  @param metadata List of Seq's with subject, timestamp, triple count, user; ordered by recent first;
   */
  private def filterMetadata(
    metadata: List[Seq[Rdf#Node]],
    request:  HTTPrequest): List[Seq[Rdf#Node]] = {
    val params = request.queryString

    wrapInReadTransaction {
      var filteredURIs = metadata
      for ((param0, values) <- params) {
        filteredURIs = filterOneCriterium(param0, values, filteredURIs)
      }
      filteredURIs
    }.getOrElse(metadata)
    //    } else
    //      metadata
  }

  /**
   * filter given List according to One Criterium on first column:
   *  ?URI <param> <value> .
   *  @param param0 abbreviated Turtle URI for triple predicate
   *  @param values abbreviated Turtle URI for triple object
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user;
   *         ordered by recent first;
   */
  private def filterOneCriterium(
    param0: String, values: Seq[String],
    metadata: List[Seq[Rdf#Node]]): List[Seq[Rdf#Node]] = {
    var filteredURIs = metadata
    val isSpecialParamameterAndNotURI =
      param0 === "limit" ||
        param0 === "uri" ||
        param0 === "query" ||
        param0 === "sparql" ||
        param0 === "paragraphs"
    if (!isSpecialParamameterAndNotURI) {
      val param = expandOrUnchanged(param0)
      for (value0 <- values) {
        val value = expandOrUnchanged(value0)
        logger.debug(s"filterOneCriterium: actually filter for param <$param> = <$value>")
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
    querySPARQL: String              = ""): (List[Seq[Rdf#Node]], NodeSeq) = {

    val params = request.queryString
    if (params.contains("uri")) {
      val focusURI = expandOrUnchanged(params("uri").headOption.getOrElse(""))
      logger.debug(s"""===== filterMetadataFocus: params.contains("uri") ${focusURI}""")
      val sparqlQuery = neighborhoodSearchSPARQL(focusURI)
      (
        filterMetadataSPARQL(metadata, request, sparqlQuery),
        // focus URI Pretty Printed
        makeHyperlinkForURItr(URI(focusURI), request.getLanguage(), allNamedGraph))

    } else if (params.contains("sparql") ||
      params.contains("query")) {
      logger.debug(s"""===== filterMetadataFocus: params.contains("sparql") or "query" """)
      val sparqlQuery = params.getOrElse(
        "sparql",
        params.getOrElse("query", Seq())).headOption.getOrElse("")
      //      println(s"filterMetadataFocus sparqlQuery: $sparqlQuery")
      (
        filterMetadataSPARQL(metadata, request, sparqlQuery),
        sparqlQueryButton(sparqlQuery, request))

    } else
      (metadata, NodeSeq.Empty)
  }

  /**
   * filter Metadata according to HTTP request, eg
   *  query=CONSTRUCT ...
   *  NOTE: only internal (users') data will appear, no external RDF data
   *  @param metadata List of Seq'q with subject, timestamp, triple count, user; ordered by recent first
   */
  private def filterMetadataSPARQL(
    metadata: List[Seq[Rdf#Node]],
    request:  HTTPrequest, querySPARQL: String = ""): List[Seq[Rdf#Node]] = {
    logger.debug(s"""===== filterMetadataSPARQL: querySPARQL $querySPARQL""")

    val results = sparqlSelectQuery(
      querySPARQL,
      context = request.queryString2)
    logger.trace(s"""===== filterMetadataSPARQL: results: $results """)

    val uris = results.get.map {
      l => l.headOption.getOrElse(nullURI)
    }.toSet

    logger.trace(
      s"""===== filterMetadataSPARQL: sparql Select result: uris $uris""")
    logger.debug(s"===== filterMetadataSPARQL: metadata.size ${metadata.size}")

    /* merge URI's from query with metadata:
     * filter metadata with URI's in SPARQL result */
    metadata.filter {
      row =>
        val uri = row(0)
        logger.debug(s"""== uri $uri""")
        uris.contains(uri)
    }
  }

  private def paragrahsView(rows: List[Seq[Rdf#Node]], request: HTTPrequest): NodeSeq = {
    val subjectsURIhtml = for (row <- rows) yield {
      try {
        logger.debug("row " + row(1).toString())
        val subjectURI = row(0)
        val lang = request.getLanguage()
        val formSyntax = {
          val formSyntax = createFormTR(subjectURI)(allNamedGraph, lang)
          filterOutFields(formSyntax)
          abbreviateLiterals(formSyntax)
        }
//        println(s"""formSyntax ${
//          formSyntax.fields.filter(
//              e => e.property.toString().contains("encoded"))}""")
        val nodesAsXHTML = generateHTMLJustFields(formSyntax, request = request)
        linkToFormSubject(formSyntax, lang) ++
          nodesAsXHTML ++
          <hr class="sf-paragraphs-separator"/>
      } catch {
        case t: Throwable =>
          t.printStackTrace()
          <p>{ t.getLocalizedMessage }</p>
      }
    }
    <br/>
    <span class="sf-values-group-inline">{
      subjectsURIhtml.flatten
    }</span>
  }

  /** filter Out Fields nor suitable for inline summary view */
  private def filterOutFields(formSyntax: FormSyntax): Unit = {
    formSyntax.fields = formSyntax.fields.filterNot(
      field =>
        field.property == rdfs.label ||
          field.property == form("separator_props_From_Subject") ||
          field.property == form("separator_props_From_Classes") ||
          field.property == form("linksCount"))
  }

  /** abbreviate literal values (eg for SIOC Posts) */
  private def abbreviateLiterals(formSyntax: FormSyntax): FormSyntax = {
    val newFields: Seq[Entry] = for (field: Entry <- formSyntax.fields) yield {
      (field, field.value) match {
        case (r: ResourceEntry, _) => r
        case (l: LiteralEntry, value : Rdf#Literal) =>
          l.valueLabel
            match {
              case s: String if (s.length() > 0) =>
                val length = s.length()
                var valueLabel = s.substring(0, Math.min(length, 200))
                if (length > 205) valueLabel = valueLabel + " ..."
                valueLabel = valueLabel.replaceAll("<p>", "")
                val newEntry = l.copy(
                    value = Literal(valueLabel),
                    valueLabel = valueLabel
                  )
                newEntry
              case _ =>
                l
            }
        case (l: LiteralEntry, _) => NullLiteralEntry
      }
    }
    formSyntax.copy(fields = newFields)
  }
}
