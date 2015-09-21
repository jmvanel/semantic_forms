package deductions.runtime.html

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import scala.xml.PrettyPrinter
import org.apache.log4j.Logger
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.Timer
import scala.util.Success

/**
 * Form for a subject URI with existing triples;
 *  a facade that blends:
 *  - the RDF cache [[deductions.runtime.sparql_cache.RDFCacheAlgo]],
 *  - the generic Form Factory [[deductions.runtime.abstract_syntax.FormSyntaxFactory]],
 *  - the HTML renderer [[Form2HTML]];
 *  transactional
 *
 * named TableView because originally it was an HTML table.
 */
trait TableViewModule[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with FormSyntaxFactory[Rdf, DATASET]
    with Timer {
  import ops._
  import rdfStore.transactorSyntax._

//  val nullURI: Rdf#URI = URI("")
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * wrapper for htmlForm that shows Failure's ;
   *  non TRANSACTIONAL
   */
  def htmlFormElemRaw(uri: String, unionGraph: Rdf#Graph=allNamedGraph, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: String = fromUri(nullURI)): NodeSeq = {

    htmlFormRaw(uri, unionGraph, hrefPrefix, blankNode, editable, actionURI,
      lang, graphURI, actionURI2, URI(formGroup)) match {
        case Success(e) => e
        case Failure(e) => <p>htmlFormElem: Exception occured: { e }</p>
      }
  }
  
  /**
   * wrapper for htmlForm that shows Failure's ;
   *  TRANSACTIONAL
   */
  def htmlFormElem(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: String = fromUri(nullURI)): NodeSeq = {

    htmlForm(uri, hrefPrefix, blankNode, editable, actionURI,
      lang, graphURI, actionURI2, URI(formGroup)) match {
        case Success(e) => e
        case Failure(e) => <p>htmlFormElem: Exception occured: { e }</p>
      }
  }

  /**
   * wrapper for htmlForm, but generates Just Fields; also shows Failure's;
   *  see [[deductions.runtime.html.Form2HTML]] .generateHTMLJustFields()
   */
  def htmlFormElemJustFields(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en",
    graphURI: String = "",
    formGroup: String = fromUri(nullURI))
    (implicit graph: Rdf#Graph)
    : NodeSeq = {

    // TODO for comprehension like in htmlForm()

    val (graphURIActual, _) = doRetrieveURI(uri, blankNode, graphURI)
    val htmlFormTry = dataset.r({
      val form = createAbstractForm(allNamedGraph, uri, editable, lang, blankNode,
        URI(formGroup))
      // new Form2HTMLBanana[Rdf] {}. // TODO
      new Form2HTML[Rdf#Node, Rdf#URI] {
        override def toPlainString(n: Rdf#Node): String =
          foldNode(n)(fromUri(_), fromBNode(_), fromLiteral(_)._1)
      }.
        generateHTMLJustFields(form,
          hrefPrefix, editable, graphURIActual)
    })
    htmlFormTry match {
      case Success(e) => e
      case Failure(e) => <p class="error">htmlFormElemJustFields: Exception occured: { e }</p>
    }
  }

  /**
   * create a form for given URI with background knowledge in RDFStoreObject.store;
   *  by default user inputs will be saved in named graph uri, except if given graphURI argument;
   *  @param blankNode if "true" given uri is a blanknode
   *  NON TRANSACTIONAL
   */
  private def htmlFormRaw(uri: String, unionGraph: Rdf#Graph=allNamedGraph, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI)
    : Try[NodeSeq] = {

    println( s"htmlFormRaw dataset $dataset" )
    val tryGraph = if (blankNode != "true") {
    	val res = retrieveURINoTransaction(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After retrieveURI(makeUri($uri), store)")
      res
    } else Success(emptyGraph)
    val graphURIActual = if (graphURI == "") uri else graphURI
    Success(graf2form( unionGraph, uri, hrefPrefix, blankNode, editable,
          actionURI, lang, graphURIActual, actionURI2, formGroup))
  }
  
  /**
   * create a form for given URI with background knowledge in RDFStoreObject.store;
   *  by default user inputs will be saved in named graph uri, except if given graphURI argument;
   *  @param blankNode if "true" given uri is a blanknode
   *  TRANSACTIONAL
   */
  private def htmlForm(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en",
    graphURI: String = "",
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI)
    : Try[NodeSeq] = {

    println( s"htmlForm dataset $dataset" )

    for {
      (graphURIActual, tryGraph) <- Try { time("doRetrieveURI", doRetrieveURI(uri, blankNode, graphURI)) }

      // TODO find another way of reporting download failures: 
      //      graphDownloaded <- tryGraph
      
      form <- dataset.rw({
        graf2form(allNamedGraph, uri, hrefPrefix, blankNode, editable,
          actionURI, lang, graphURIActual, actionURI2, formGroup)
      })
    } yield form
  }

  /**
   * with transaction
   *
   *  @return Actual graph URI: given graph URI or else given uri
   */
  private def doRetrieveURI(uri: String, blankNode: String, graphURI: String): (String, Try[Rdf#Graph]) = {
    val tryGraph = if (blankNode != "true") {
      val res = retrieveURI(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After retrieveURI(makeUri($uri), store)")
      res
    } else Success(emptyGraph)
    val graphURIActual = if (graphURI == "") uri else graphURI
    (graphURIActual, tryGraph)
  }

  /**
   * create a form for given URI resource (instance) with background knowledge
   *  in given graph
   *  TODO non blocking
   */
  private def graf2form(graphe: Rdf#Graph, uri: String,
    hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en", graphURI: String,
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI)
    : NodeSeq = {

    implicit val graph: Rdf#Graph = graphe
    println(s"graf2form(graph: graph size: ${graph.size}")
    val form = time("createAbstractForm",
      createAbstractForm(graph, uri, editable, lang, blankNode, formGroup))
    val htmlFormGen = time("new Form2HTML",
      // new Form2HTMLBanana[Rdf] {}. // TODO
      new Form2HTML[Rdf#Node, Rdf#URI] {
        override def toPlainString(n: Rdf#Node): String =
          foldNode(n)(fromUri(_), fromBNode(_), fromLiteral(_)._1)
      }
    )
    val htmlForm = htmlFormGen.
      generateHTML(form, hrefPrefix, editable, actionURI, graphURI,
        actionURI2)
    htmlForm
  }

  private def createAbstractForm(graphArg: Rdf#Graph, uri: String, editable: Boolean,
    lang: String, blankNode: String, formGroup: Rdf#URI)
    (implicit graph: Rdf#Graph)
    : FormModule[Rdf#Node, Rdf#URI]#FormSyntax = {
    val subjectNnode = if (blankNode == "true")
      /* TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme) */
      BNode(uri)
    else URI(uri)
    val factory = this
//      new FormSyntaxFactory[Rdf, DATASET] {
//     val graph=graphArg
//     val preferedLanguage = lang }
    preferedLanguage = lang
    factory.createForm(subjectNnode, editable, formGroup)
  }

  def htmlFormString(uri: String,
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String)(implicit allNamedGraphs: Rdf#Graph): String = {
    val f = htmlFormElem(uri, editable = editable, actionURI = actionURI)
    val pp = new PrettyPrinter(80, 2)
    pp.formatNodes(f)
  }

  def graf2formString(graph1: Rdf#Graph, uri: String, graphURI: String): String = {
    graf2form(graph1, uri, graphURI = graphURI).toString
  }

  //  override 
  def toPlainString(n: Rdf#Node): String = {
    val v = foldNode(n)(
      uri => fromUri(uri),
      bn => fromBNode(bn),
      lit => { val (v, typ, langOption) = fromLiteral(lit); v }
    )
    v
  }

}
