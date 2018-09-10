package deductions.runtime.services.html

import deductions.runtime.data_cleaning.BlankNodeCleanerIncremental
import deductions.runtime.html.logger
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.sparql_cache.algos.StatisticsGraph
import deductions.runtime.core.HTTPrequest
import deductions.runtime.views.FormHeader
import org.w3.banana.RDF

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, Text}
import deductions.runtime.user.FoafProfileClaim

import scalaz._
import Scalaz._

trait TriplesViewWithTitle[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with TriplesViewModule[Rdf, DATASET]
    with FormHeader[Rdf, DATASET]
    with StatisticsGraph[Rdf, DATASET]
    with BlankNodeCleanerIncremental[Rdf, DATASET]
    with FoafProfileClaim[Rdf, DATASET] {
  
    import config._
    import ops._

  /**
   * Naked form from [[TriplesViewModule]], plus:
   *
   * - title and links on top of the form,
   * - page URI (graph) statistics,
   * - behavior (manageBlankNodesReload, Exception management)
   *
   * (called for /display service)
   *
   * @return couple with XHTML form and Boolean whether  subject type did Change
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
               request: HTTPrequest
  ): (NodeSeq, Boolean) = {
    logger.info(
      s"""ApplicationFacadeImpl.htmlForm URI <$uri0> blankNode "$blankNode"
              editable=$editable lang=$lang graphURI <$graphURI>""")
    val uri = uri0.trim()
    var typeChange = false
    if (uri =/= null && uri =/= "")
      try {
        val datasetOrDefault = getDatasetOrDefault(database)
        val result: Try[NodeSeq] = {

          // 1. retrieve or check URI from Internet

          val (tryGraph: Try[Rdf#Graph], failureOrStatistics: NodeSeq ) =
            if (blankNode =/= "true") {
                // TODO pass datasetOrDefault)
              val tryGraph = retrieveURIBody(
                makeUri(uri), datasetOrDefault, request, transactionsInside=true)
              val failureOrStatistics = tryGraph match {
                case Failure(e) => <p>{ e.getLocalizedMessage }</p>
                case Success(g) =>
                  val res = wrapInReadTransaction{ formatHTMLStatistics(URI(uri), g, request) }
                  res match {
                    case Success(xmlNodes) => xmlNodes
                    case Failure(e) => <p> Error in formatHTMLStatistics: {e}</p>
                  }
              }
              (tryGraph, failureOrStatistics)
            } else
              (Success(emptyGraph), "")


          // LOGGING only

          tryGraph match {
            case Success(gr) =>

              logger.debug(s"htmlForm: Success !!!!!!!!!!")
              wrapInReadTransaction {
                // FEATURE: annotate plain Web site
                // TODO use exists( triple => ... )
                typeChange = gr.size === 1 && gr.triples.head.objectt == foaf.Document
                logger.debug(s">>>> htmlForm typeChange $typeChange")
              }

              import scala.concurrent.ExecutionContext.Implicits.global
              Future {
                wrapInTransaction {
                  // TODO should be done in FormSaver
                  logger.debug(s"Search in <$uri> duplicate graph rooted at blank node: size " +
                    ops.getTriples(gr).size)
                  manageBlankNodesReload(gr,
                    URI(uri), datasetOrDefault)
                }
              }
            case Failure(f) => logger.error(s"htmlForm: ERROR: typeChange, manageBlankNodesReload: $f")
          }

          // 2. generate form and its header

          // FEATURE: annotate plain Web site
          val editable2 = editable || typeChange

          implicit val graph = allNamedGraph
          val (formItself, formSyntax) =
            htmlFormElemRaw(
              uri, graph, hrefDisplayPrefix, blankNode, editable = editable2,
              lang = lang,
              formuri = formuri,
              graphURI = graphURI,
              database = database,
              request = request, inputGraph = tryGraph)
            logger.debug(s">>>> after htmlFormElemRaw, formSyntax $formSyntax")

            wrapInTransaction({  // or wrapInReadTransaction ?
              Text("\n") ++
              titleEditDisplayDownloadLinksThumbnail(formSyntax, lang, editable2, request) ++
              <div class="col-xs-12">
                <!--++ div wraps failure Or Statistics form header (form generation traceability) ++-->
                { failureOrStatistics }</div> ++
              profileClaimUI(request) ++
              formItself
          }, datasetOrDefault)
        }

        val resultXML =  <div> {result.get } </div>

        (resultXML, typeChange)
      } catch {
        case e: Exception => // e.g. org.apache.jena.riot.RiotException

        // 3. display errors

          (<p class="sf-error-message">
            <pre>Error in TriplesViewWithTitle.htmlForm()
        		  <br/>
              {
                "Exception class " + e.getClass + " -\n"
                e.getLocalizedMessage() + "\n" + printTrace(e).replaceAll("\\)", ")\n")
              }<br/>
              Cause:{ if (e.getCause() != null) e.getCause().getLocalizedMessage() }
            </pre>
          </p>, false)
      }
    else
      (<div class="row">Enter an URI</div>, false)
  }

  /** PASTED !!! */
  def printTrace(e: Exception): String = {
    var s = ""
    for (elem <- e.getStackTrace()) { s = s + " " + elem }
    s
  }
}
