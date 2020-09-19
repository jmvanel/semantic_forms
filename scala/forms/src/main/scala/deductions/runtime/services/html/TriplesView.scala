package deductions.runtime.services.html

import deductions.runtime.abstract_syntax.{FormSyntaxFactory, UserTraceability}
import deductions.runtime.html.{HtmlGeneratorInterface, logger}
import deductions.runtime.semlogs.TimeSeries
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.{Configuration, RDFPrefixesInterface, Timer}
import deductions.runtime.core.HTTPrequest
import deductions.runtime.views.TableFromListListRDFNodes
import org.w3.banana.RDF

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq
import deductions.runtime.data_cleaning.BlankNodeCleaner
import deductions.runtime.utils.CSSClasses

import scalaz._
import Scalaz._

/**
 * Form for a subject URI with existing triples;
 *  a facade that blends:
 *  - the RDF cache deductions.runtime.sparql_cache.RDFCacheAlgo,
 *  - the generic Form Factory deductions.runtime.abstract_syntax.FormSyntaxFactory,
 *  - the HTML renderer deductions.runtime.html.Form2HTML;
 *  transactional
 *
 * Was named TableView because originally it was an HTML table.
 */
trait TriplesViewModule[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with FormSyntaxFactory[Rdf, DATASET]
    with TimeSeries[Rdf, DATASET]
    with TableFromListListRDFNodes[Rdf]
    with UserTraceability[Rdf, DATASET]
//    with BlankNodeCleaner[Rdf, DATASET]
    with CSSClasses
    with Timer {

  val config: Configuration
  val htmlGenerator: HtmlGeneratorInterface[Rdf#Node, Rdf#URI] // Form2HTMLBanana[Rdf]
  import htmlGenerator._
  import ops._

  /**
   * wrapper for htmlForm that shows Failure's ;
   * TRANSACTIONAL
   */
  def htmlFormElemRaw(uri: String, unionGraph: Rdf#Graph=allNamedGraph,
    hrefPrefix: String = config.hrefDisplayPrefix, blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: String = fromUri(nullURI),
    formuri: String="",
    database: String = "TDB",
    request: HTTPrequest,
    inputGraph: Try[Rdf#Graph] = Success(emptyGraph)
  ): ( NodeSeq, FormSyntax ) = {
    htmlFormRawTry(uri, unionGraph, hrefPrefix, blankNode, editable, actionURI,
      graphURI, actionURI2, URI(formGroup), formuri, database, request, inputGraph) match {
        case Success(e) => e
        case Failure(e) => ( <p>htmlFormElem: Exception occured: { e }</p>, FormSyntax(nullURI, Seq() ) )
      }
  }
  
  /**
   * wrapper for htmlForm that shows Failure's ;
   *  TRANSACTIONAL
   */
  def htmlFormElem(uri: String, hrefPrefix: String = config.hrefDisplayPrefix, blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: String = fromUri(nullURI),
    request: HTTPrequest
    ): NodeSeq = {
    val lang = request.getLanguage()
    htmlForm(uri, hrefPrefix, blankNode, editable, actionURI,
      graphURI, actionURI2, URI(formGroup), request) match {
        case Success(e) => e._1
        case Failure(e) => <p>htmlFormElem: Exception occured: { e }</p>
      }
  }

  /**
   * wrapper for htmlForm, but generates Just Fields; also shows Failure's;
   * see deductions.runtime.html.Form2HTML.generateHTMLJustFields() .
   * TRANSACTIONAL
   */
  def htmlFormElemJustFields(uri: String, hrefPrefix: String = config.hrefDisplayPrefix, blankNode: String = "",
    editable: Boolean = false,
    lang0: String = "en",
    graphURI: String = "",
    formGroup: String = fromUri(nullURI),
    formuri: String="" )
    : NodeSeq = {

    implicit val lang = lang0

    // TODO for comprehension like in htmlForm()

println( s">>>> htmlFormElemJustFields 0" )
    val (graphURIActual, _) = doRetrieveURI(uri, blankNode, graphURI)
//    val htmlFormTry = rdfStore.rw( dataset, {
//println( s">>>> htmlFormElemJustFields 1" )
      implicit val graph: Rdf#Graph = allNamedGraph
println( s">>>> htmlFormElemJustFields 2 " )
      val ops1 = ops
      val config1 = config
      val form = createAbstractForm(
          uri, editable, blankNode,
          URI(formGroup), formuri )
//println( s">>>> htmlFormElemJustFields $form" )

    val htmlFormTry = rdfStore.rw( dataset, {
        generateHTMLJustFields(form,
          hrefPrefix, editable, graphURIActual, request=HTTPrequest() )
    })
    htmlFormTry match {
      case Success(e) => e
      case Failure(e) => <p class="error">htmlFormElemJustFields: Exception occured: {
        e.printStackTrace()
      }</p>
      throw e
    }
  }

  /** create HTML Form or view From SPARQL CONSTRUCT query */
  def createHTMLFormFromSPARQL(query: String,
                               editable: Boolean = false,
                               formuri: String = "", request: HTTPrequest): NodeSeq = {
    val formSyntax = createFormFromSPARQL(query, editable, formuri, request)
//    println(s">>>> createHTMLFormFromSPARQL: formSyntax $formSyntax")
    generateHTML(formSyntax, request=request, hrefPrefix = config.hrefDisplayPrefix,
                   cssForURI = "",
                   cssForProperty = "")
  }


  /**
   * create a form for given URI with background knowledge in RDFStoreObject.store;
   *  by default user inputs will be saved in named graph uri, except if given graphURI argument;
   * TRANSACTIONAL;
   *  
   *  Note: first try to retrieve from Internet at given URI,
   *  then eventually save in TDB,
   *  then read again <uri> ?P ?O.	from TDB, in any named graph,
   *  to catch 1) triples downloaded from URI, 2) triples preloaded,
   *  3) triples coming from user edits
   *  @param blankNode if "true" given uri is a blanknode
   *  @param inputGraph retrieved or downloaded graph about subject URI - unused
   */
  private def htmlFormRawTry(uri: String, unionGraph: Rdf#Graph = allNamedGraph,
                          hrefPrefix: String = config.hrefDisplayPrefix,
                          blankNode: String = "",
                          editable: Boolean = false,
                          actionURI: String = "/save",
                          graphURI: String = "",
                          actionURI2: String = "/save",
                          formGroup: Rdf#URI = nullURI,
                          formuri: String="",
                          database: String = "TDB",
                          request: HTTPrequest,
                          inputGraph: Try[Rdf#Graph] = Success(emptyGraph)
		  ): Try[( NodeSeq, FormSyntax)] = {

    logger.debug(
        s">>>> htmlFormRawTry: getMetadataAboutSubject($uri) = ${makeHtmlTable( getMetadataAboutSubject(URI(uri)) )}")
    logger.debug(
      s"htmlFormRawTry dataset $dataset, graphURI <$graphURI>")

    val graphURIActual = if (graphURI === "") uri else graphURI
    Success(graf2form(unionGraph, uri, hrefPrefix, blankNode, editable,
      actionURI, graphURIActual, actionURI2, formGroup, formuri, request))
  }
  
  /**
   * create a form for given URI with background knowledge in RDFStoreObject.store;
   *  by default user inputs will be saved in named graph uri, except if given graphURI argument;
   *  @param blankNode if "true" given uri is a blanknode
   *  TRANSACTIONAL
   */
  private def htmlForm(uri: String, hrefPrefix: String = config.hrefDisplayPrefix, blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI,
    request: HTTPrequest
)
    : Try[( NodeSeq, FormSyntax )] = {

    println( s"htmlForm: dataset $dataset" )

    for {
      (graphURIActual, tryGraph) <- Try {
        time("doRetrieveURI", doRetrieveURI(uri, blankNode, graphURI))
      }
      // TODO find another way of reporting download failures: 
      //      graphDownloaded <- tryGraph
        lang = request.getLanguage()
        form = graf2form(allNamedGraph, uri, hrefPrefix, blankNode, editable,
          actionURI, graphURIActual, actionURI2, formGroup, "", request)
    } yield form
  }

  /** Retrieve URI from Internet or triples cache;
   * with transaction
   *
   *  @return Actual graph URI: given graph URI or if not specified given uri
   */
  private def doRetrieveURI(uri: String, blankNode: String, graphURI: String): (String, Try[Rdf#Graph]) = {
    val tryGraph = if (blankNode != "true") {
      val res = retrieveURI(makeUri(uri), dataset)
      logger.info(s"After retrieveURI(makeUri($uri), dataset) isSuccess ${res.isSuccess}")
      res
    } else Success(emptyGraph)
    val graphURIActual = if (graphURI === "") uri else graphURI
    (graphURIActual, tryGraph)
  }

  /**
   * create a form for given URI resource (instance) with background knowledge
   *  in given graph
   *  TODO non blocking
   * TRANSACTIONAL;
   */
  private def graf2form(graphe: Rdf#Graph, uri: String,
    hrefPrefix: String = config.hrefDisplayPrefix, blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    graphURI: String,
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI,
    formuri: String="",
    request: HTTPrequest
  ) : ( NodeSeq , FormSyntax ) = {

    implicit val graph: Rdf#Graph = graphe
    implicit val lang = request.getLanguage

    lazy val formSyntax = time("createAbstractForm",
      createAbstractForm(uri, editable, blankNode, formGroup, formuri))

    lazy val formSyntaxWithInfo = addUserInfoOnTriples(formSyntax)
    lazy val formSyntaxWithInfoAndFixedValues = fixValues( formSyntaxWithInfo, request )

    val htmlForm =
      generateHTML(formSyntaxWithInfoAndFixedValues, hrefPrefix, editable, actionURI, graphURI,
        actionURI2, request,
        cssForURI = "sf-value-block",
        cssForProperty = cssClasses.formLabelCSSClass
        // "col-xs-3 col-sm-2 col-md-2 control-label"
        )
    ( htmlForm, formSyntaxWithInfoAndFixedValues )
  }

  /** calls createFormTR; TRANSACTIONAL */
  private def createAbstractForm(
      uri: String, editable: Boolean,
      blankNode: String, formGroup: Rdf#URI, formuri: String="")
    (implicit graph: Rdf#Graph, lang: String )
    : FormSyntax = {
    val subjectNode = if (blankNode === "true")
      /* Jena TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme) */
      BNode(uri)
    else URI(uri)

    createFormTR(subjectNode, editable, formGroup, formuri)
  }

}
