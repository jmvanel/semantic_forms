package deductions.runtime.html

import scala.xml.Elem
import org.apache.log4j.Logger
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCache
import scala.xml.PrettyPrinter
import deductions.runtime.uri_classify.SemanticURIGuesser
import scala.concurrent.Future
import deductions.runtime.utils.MonadicHelpers
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import org.w3.banana.RDFOps
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.FormModule

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

  /** create a form for given URI with background knowledge in RDFStoreObject.store  */
  def htmlForm(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en"): Try[Elem] = {
    val dataset = RDFStoreObject.dataset
    if (blankNode != "true") {
      retrieveURI(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After retrieveURI(makeUri($uri), store)")
    }
    val r = rdfStore.r(dataset, {
      for (
        // TODO use allNamedGraphs from RDFStoreObject
        allNamedGraphs <- rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph"))
      ) yield graf2form(allNamedGraphs, uri, hrefPrefix, blankNode, editable, actionURI, lang)
    })
    //    MonadicHelpers.tryToFutureFlat(r)
    r.flatMap { identity }
  }

  def htmlFormElem(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    actionURI: String = "/save",
    lang: String = "en"): Elem = {
    //    Await.result(
    htmlForm(uri, hrefPrefix, blankNode, editable, actionURI, lang) match {
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
    lang: String = "en"): Elem = {
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
      form, hrefPrefix, editable, actionURI)
    htmlForm
  }

  def htmlFormString(uri: String,
    editable: Boolean = false,
    actionURI: String = "/save"): String = {
    val f = htmlFormElem(uri, editable = editable, actionURI = actionURI)
    val pp = new PrettyPrinter(80, 2)
    pp.format(f)
  }

  def graf2formString(graph1: Rdf#Graph, uri: String): String = {
    graf2form(graph1, uri).toString
  }

  /**
   * From the list of ?O such that uri ?P ?O ,
   *  return the list of their SemanticURIType as a Future
   */
  def getSemanticURItypes(uri: String): Future[Iterator[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]] = {
    // get the list of ?O such that uri ?P ?O .
    import scala.concurrent.ExecutionContext.Implicits.global
    val dataset = RDFStoreObject.dataset
    //  TODO val graphFuture =  RDFStoreObject.allNamedGraphsFuture

    val r = rdfStore.r(dataset, {
      for (
        // TODO use allNamedGraphs from RDFStoreObject
        allNamedGraphs <- rdfStore.getGraph(dataset, ops.makeUri("urn:x-arq:UnionGraph"))
      ) yield {
        val triples: Iterator[Rdf#Triple] = ops.find(allNamedGraphs,
          ops.makeUri(uri), ANY, ANY)
        val semanticURItypes =
          for (triple <- triples) yield {
            val node = triple.getObject
            val semanticURItype = if (isDereferenceableURI(node)) {
              SemanticURIGuesser.guessSemanticURIType(node.toString())
            } else
              Future.successful(SemanticURIGuesser.Unknown) // TODO NONE
            semanticURItype.map { st => (node, st) }
          }
        Future sequence semanticURItypes
      }
    })
    val r1 = r.flatMap(identity)
    val rr = MonadicHelpers.tryToFuture(r1)
    rr.flatMap(identity)
  }

  def isDereferenceableURI(node: Rdf#Node) = {
    if (isURI(node)) {
      val uri = node.toString()
      uri.startsWith("http:") ||
        uri.startsWith("https:") ||
        uri.startsWith("ftp:")
    } else false
  }

  private def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None
}