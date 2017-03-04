package deductions.runtime.html

import scala.util.Failure
import scala.util.Success
import scala.xml.NodeSeq
import scala.xml.Text

import org.w3.banana.RDF

import deductions.runtime.data_cleaning.BlankNodeCleanerIncremental
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.sparql_cache.algos.StatisticsGraph
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.views.FormHeader
import scala.util.Try
import scala.concurrent.Future

trait TriplesViewWithTitle[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with TriplesViewModule[Rdf, DATASET]
    with FormHeader[Rdf]
    with StatisticsGraph[Rdf]
    with BlankNodeCleanerIncremental[Rdf, DATASET] {
  
    import config._
    import ops._

  /**
   * Naked form from [[TriplesViewModule]], plus:
   *
   * - title and links on top of the form,
   * - page URI (graph) statistics,
   * - behavior (manageBlankNodesReload, Exception management)
   *
   * TRANSACTIONAL
   */
  def htmlForm(uri0: String,
               blankNode: String = "",
               editable: Boolean = false,
               lang: String = "en",
               formuri: String = "",
               graphURI: String = "",
               database: String = "TDB",
               request: HTTPrequest = HTTPrequest()): NodeSeq = {
    logger.info(
      s"""ApplicationFacadeImpl.htmlForm URI <$uri0> blankNode "$blankNode"
              editable=$editable lang=$lang graphURI <$graphURI>""")
    val uri = uri0.trim()
    if (uri != null && uri != "")
      try {
        val datasetOrDefault = getDatasetOrDefault(database)
        val result: Try[NodeSeq] = {

          val t1 = wrapInTransaction({

            if (blankNode != "true") {
              val resRetrieve = retrieveURINoTransaction( // if( blankNode=="true") makeUri("_:" + uri ) else makeUri(uri),
                makeUri(uri), datasetOrDefault)

              val failureOrStatistics = resRetrieve match {
                case Failure(e) => e.getLocalizedMessage
                case Success(g) => formatHTMLStatistics(URI(uri), g, lang)
              }
              (resRetrieve, failureOrStatistics)
            } else
              (Success(emptyGraph), "")
          }, datasetOrDefault)

          val (tryGraph: Try[Rdf#Graph],
            failureOrStatistics /* String or NodeSeq */ ) = t1.get

          tryGraph match {
            case Success(gr) =>
              import scala.concurrent.ExecutionContext.Implicits.global
              Future {
                // TODO should be done in FormSaver 
                println(s"Search in <$uri> duplicate graph rooted at blank node: size " +
                  ops.getTriples(gr).size)
                manageBlankNodesReload(gr,
                  URI(uri), datasetOrDefault)
              }
            case Failure(f) => logger.error(s"manageBlankNodesReload: $f")
          }

          // or wrapInReadTransaction ?
          wrapInTransaction({
            implicit val graph = allNamedGraph
            val formBoth = htmlFormElemRaw(uri, graph, hrefDisplayPrefix, blankNode, editable = editable,
              lang = lang,
              formuri = formuri,
              graphURI = graphURI,
              database = database,
              request = request, inputGraph = tryGraph)
            println(s">>>> after htmlFormElemRaw")
            val formItself = formBoth._1
            val formSyntax = formBoth._2

            Text("\n") ++
              titleEditDisplayDownloadLinksThumbnail(formSyntax, lang, editable) ++
              <div>{ failureOrStatistics }</div> ++
              formItself
          }, datasetOrDefault)
        }

        result.get
      } catch {
        case e: Exception => // e.g. org.apache.jena.riot.RiotException
          <p class="sf-error-message">
            <pre>
              {
                e.getLocalizedMessage() + "\n" + printTrace(e).replaceAll("\\)", ")\n")
              }<br/>
              Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
            </pre>
          </p>
      }
    else
      <div class="row">Enter an URI</div>
  }

  private  def htmlForm_OLD(uri0: String,
		           blankNode: String = "",
               editable: Boolean = false,
               lang: String = "en",
               formuri: String="",
               graphURI: String = "",
               database: String = "TDB",
               request:HTTPrequest = HTTPrequest() )
  : NodeSeq = {
    logger.info(
        s"""ApplicationFacadeImpl.htmlForm URI <$uri0> blankNode "$blankNode"
              editable=$editable lang=$lang graphURI <$graphURI>""")
    val uri = uri0.trim()
    if (uri != null && uri != "")
      try {
        val datasetOrDefault = getDatasetOrDefault(database)
        val result: Try[NodeSeq] = rdfStore.rw(datasetOrDefault, {
          val (tryGraph: Try[Rdf#Graph],
            failureOrStatistics /* String or NodeSeq */ ) =
            if (blankNode != "true") {
              val resRetrieve = retrieveURINoTransaction( // if( blankNode=="true") makeUri("_:" + uri ) else makeUri(uri),
                makeUri(uri), datasetOrDefault)

              // TODO should be done in FormSaver 
              println(s"Search in <$uri> duplicate graph rooted at blank node: size " +
                ops.getTriples(resRetrieve.get).size)
              manageBlankNodesReload(resRetrieve.getOrElse(emptyGraph),
                URI(uri), datasetOrDefault)

              val failureOrStatistics = resRetrieve match {
                case Failure(e) => e.getLocalizedMessage
                case Success(g) => formatHTMLStatistics(URI(uri), g, lang)
              }
              (resRetrieve, failureOrStatistics)
            } else
              (Success(emptyGraph), "")

          implicit val graph = allNamedGraph
          val formBoth = htmlFormElemRaw(uri, graph, hrefDisplayPrefix, blankNode, editable = editable,
            lang = lang,
            formuri = formuri,
            graphURI = graphURI,
            database = database,
            request = request, inputGraph = tryGraph)
          println(s">>>> after htmlFormElemRaw")
          val formItself = formBoth._1
          val formSyntax = formBoth._2

          Text("\n") ++
            titleEditDisplayDownloadLinksThumbnail(formSyntax, lang, editable) ++
            <div>{ failureOrStatistics }</div> ++
            formItself
        })
        result.get
      } catch {
        case e: Exception => // e.g. org.apache.jena.riot.RiotException
          <p class="sf-error-message">
            <pre>
              {
                e.getLocalizedMessage() + "\n" + printTrace(e).replaceAll("\\)", ")\n")
              }<br/>
              Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
            </pre>
          </p>
      }
    else
      <div class="row">Enter an URI</div>
  }

  /** PASTED !!! */
  def printTrace(e: Exception): String = {
    var s = ""
    for (elem <- e.getStackTrace()) { s = s + " " + elem }
    s
  }
}