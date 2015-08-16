package deductions.runtime.html

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.Elem
import scala.xml.PrettyPrinter
import org.apache.log4j.Logger
import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.jena.RDFStoreObject
import scala.xml.NodeSeq
import org.w3.banana.SparqlGraphModule
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.abstract_syntax.FormModule
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFOpsModule

/**
 * TODO : move to package jena
 */
trait TableView extends JenaModule with TableViewModule[Jena, Dataset]

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
    extends //    RDFOpsModule
    //    with 
    RDFCacheAlgo[Rdf, DATASET] //    with SparqlGraphModule
    {
  import ops._
  import rdfStore.transactorSyntax._

  val nullURI: Rdf#URI = ops.URI("")
  import scala.concurrent.ExecutionContext.Implicits.global

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
    formGroup: Rdf#URI = nullURI): Try[NodeSeq] = {
    val graphURIActual = doRetrieveURI(uri, blankNode, graphURI)
    dataset.r({
      graf2form(allNamedGraph, uri, hrefPrefix, blankNode, editable,
        actionURI, lang, graphURIActual, actionURI2, formGroup)
    })
  }

  /** @return Actual graph URI: given graph URI or else given uri */
  private def doRetrieveURI(uri: String, blankNode: String, graphURI: String) = {
    if (blankNode != "true") {
      retrieveURI(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After retrieveURI(makeUri($uri), store)")
    }
    if (graphURI == "") uri else graphURI
  }

  /** wrapper for htmlForm that shows Failure's */
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
        case Failure(e) => <p>Exception occured: { e }</p>
      }
  }

  /**
   * wrapper for htmlForm, but Just Fields; shows Failure's;
   *  see [[deductions.runtime.html.Form2HTML]] .generateHTMLJustFields()
   */
  def htmlFormElemJustFields(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en",
    graphURI: String = "",
    formGroup: String = fromUri(nullURI)): NodeSeq = {

    val graphURIActual = doRetrieveURI(uri, blankNode, graphURI)
    val htmlFormTry = dataset.r({
      val form = createAbstractForm(allNamedGraph, uri, editable, lang, blankNode,
        URI(formGroup))
      new Form2HTML[Rdf#Node, Rdf#URI] {}.
        generateHTMLJustFields(form,
          hrefPrefix, editable, graphURIActual)
    })
    htmlFormTry match {
      case Success(e) => e
      case Failure(e) => <p>Exception occured: { e }</p>
    }
  }

  /**
   * create a form for given URI resource (instance) with background knowledge
   *  in given graph
   *  TODO non blocking
   */
  private def graf2form(graph: Rdf#Graph, uri: String,
    hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en", graphURI: String,
    actionURI2: String = "/save",
    formGroup: Rdf#URI = nullURI): NodeSeq = {
    val form = createAbstractForm(graph, uri, editable, lang, blankNode, formGroup)
    val htmlForm =
      // TODO code duplicated in trait CreationFormAlgo.create() 
      new Form2HTML[Rdf#Node, Rdf#URI] {
        override def toPlainString(n: Rdf#Node): String =
          ops.foldNode(n)(fromUri(_), fromBNode(_), fromLiteral(_)._1)
      }.
        generateHTML(form, hrefPrefix, editable, actionURI, graphURI,
          actionURI2)
    htmlForm
  }

  private def createAbstractForm(graph: Rdf#Graph, uri: String, editable: Boolean,
    lang: String, blankNode: String, formGroup: Rdf#URI): FormModule[Rdf#Node, Rdf#URI]#FormSyntax = {
    val subjectNnode = if (blankNode == "true")
      /* TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme) */
      BNode(uri)
    else URI(uri)
    val factory = new FormSyntaxFactory[Rdf](graph, preferedLanguage = lang)
    factory.createForm(subjectNnode, editable, formGroup)
  }

  def htmlFormString(uri: String,
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String): String = {
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
