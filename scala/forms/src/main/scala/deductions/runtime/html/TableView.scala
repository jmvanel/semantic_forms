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
import deductions.runtime.sparql_cache.RDFCache

/** Form for a subject URI with existing triples; transactional */
trait TableView extends TableViewModule

/** Form; transactional */
trait TableViewModule
    extends RDFModule
    with RDFCache
    //  with Form2HTML[Rdf#Node, Rdf#URI]
    with Form2HTML[Jena#Node, Jena#URI] // TODO remove Jena !!!!!!!!!!!!!!
    {
  import ops._
  val nullURI: Rdf#URI = ops.URI("")
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * create a form for given URI with background knowledge in RDFStoreObject.store;
   *  by default user inputs will be saved in named graph uri, except if given graphURI argument;
   *  TRANSACTIONAL
   */
  def htmlForm(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en", graphURI: String = ""): Try[Elem] = {

    val graphURIActual = if (graphURI == "") uri else graphURI
    val dataset = RDFStoreObject.dataset
    if (blankNode != "true") {
      retrieveURI(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After retrieveURI(makeUri($uri), store)")
    }
    val r = rdfStore.r(dataset, {
      for (
        // TODO use allNamedGraphs from RDFStoreObject
        allNamedGraphs <- rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph"))
      ) yield graf2form(allNamedGraphs, uri, hrefPrefix, blankNode, editable, actionURI, lang, graphURIActual)
    })
    //    MonadicHelpers.tryToFutureFlat(r)
    r.flatMap { identity }
  }

  /** wrapper for htmlForm that shows Failure's */
  def htmlFormElem(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en",
    graphURI: String = ""): Elem = {
    //    Await.result(
    htmlForm(uri, hrefPrefix, blankNode, editable, actionURI, lang, graphURI) match {
      case Success(e) => e
      case Failure(e) => <p>Exception occured: { e }</p>
    }
    //      , 5 seconds)
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
    lang: String = "en", graphURI: String): Elem = {

    val factory = new FormSyntaxFactory[Rdf](graph, preferedLanguage = lang)
    val form = factory.createForm(
      if (blankNode == "true")
        /* TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme) */
        BNode(uri)
      else URI(uri), editable
    )
    println("form:\n" + form)
    val htmlForm = generateHTML(
      form, hrefPrefix, editable, actionURI, graphURI)
    htmlForm
  }

  def htmlFormString(uri: String,
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String): String = {
    val f = htmlFormElem(uri, editable = editable, actionURI = actionURI)
    val pp = new PrettyPrinter(80, 2)
    pp.format(f)
  }

  def graf2formString(graph1: Rdf#Graph, uri: String, graphURI: String): String = {
    graf2form(graph1, uri, graphURI = graphURI).toString
  }

}