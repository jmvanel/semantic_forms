package deductions.runtime.html

import scala.xml.Elem
import org.apache.log4j.Logger
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCacheJena
import scala.xml.PrettyPrinter
import deductions.runtime.uri_classify.SemanticURIGuesser
import scala.concurrent.Future
import deductions.runtime.utils.MonadicHelpers
import scala.concurrent.Await
import scala.concurrent.duration._

/** Table View of a form (Jena) */
trait TableView extends TableViewModule

/** Table View of a form */
trait TableViewModule
  extends RDFModule
  with RDFCacheJena // TODO depend on generic Rdf
//  with Form2HTML[Rdf#Node, Rdf#URI]
  with Form2HTML[Jena#Node, Jena#URI]
{
  import Ops._
  val nullURI : Rdf#URI = Ops.URI( "" )
  import scala.concurrent.ExecutionContext.Implicits.global
 
  /** create a form for given uri with background knowledge in RDFStoreObject.store  */
  def htmlForm( uri:String, hrefPrefix:String="", blankNode:String="",
      editable:Boolean=false,
      actionURI:String="/save",
      lang:String="en") : Future[Elem] = {   
    val dataset =  RDFStoreObject.dataset
    if(blankNode != "true"){
      retrieveURI_new(makeUri(uri), dataset)
      Logger.getRootLogger().info(s"After storeURI(makeUri($uri), store)")
    }
    val r = rdfStore.r(dataset, {
//    store.readTransaction {
      for( 
         allNamedGraphs <- rdfStore.getGraph(makeUri("urn:x-arq:UnionGraph"))
         ) yield
      graf2form(allNamedGraphs, uri, hrefPrefix, blankNode, editable, actionURI, lang)
    })
    MonadicHelpers.tryToFutureFlat(r)
  }

  def htmlFormElem(uri: String, hrefPrefix: String = "", blankNode: String = "",
                   editable: Boolean = false,
                   actionURI: String = "/save",
                   lang: String = "en"): Elem = {
    Await.result(
      htmlForm( uri, hrefPrefix, blankNode, editable, actionURI, lang),
      5 seconds)
  }
    
  /** create a form for given URI resource (instance) with background knowledge
   *  in given graph
   *  TODO non blocking */
  private def graf2form(graph: Rdf#Graph, uri:String,
      hrefPrefix:String="", blankNode:String="",
      editable:Boolean=false,
      actionURI:String="/save",
      lang:String="en"
  ): Elem = {
    val factory = new FormSyntaxFactory[Rdf](graph, preferedLanguage=lang )
    val form = factory.createForm(
        if( blankNode == "true" )
          /* TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme) */
          BNode(uri)
        else URI(uri)
        , editable
    )
    println("form:\n" + form)
    val htmlForm = generateHTML(form, hrefPrefix, editable, actionURI )
    htmlForm
  }
  
  def htmlFormString(uri:String,
      editable:Boolean=false,
      actionURI:String="/save" ) : String = {
    val f = htmlFormElem(uri, editable=editable, actionURI=actionURI)
    val pp = new PrettyPrinter(80, 2)
    pp.format(f)
  }

  def graf2formString(graph1: Rdf#Graph, uri:String): String = {
    graf2form(graph1, uri).toString
  }

  /** From the list of ?O such that uri ?P ?O ,
   *  return the list of their SemanticURIType as a Future */
  def getSemanticURItypes(uri: String) :
	  Future[Iterator[(Rdf#Node, SemanticURIGuesser.SemanticURIType)]] = {
    // get the list of ?O such that uri ?P ?O .
    import scala.concurrent.ExecutionContext.Implicits.global
    val dataset =  RDFStoreObject.dataset
    val r = rdfStore.r(dataset, {
//    store.readTransaction {
      for( 
        allNamedGraphs <- rdfStore.getGraph(Ops.makeUri("urn:x-arq:UnionGraph"))
      ) yield { 
      val triples :Iterator[Rdf#Triple] = Ops.find(allNamedGraphs, Ops.makeUri(uri), ANY, ANY)
      val semanticURItypes =
        for (triple <- triples) yield {
          val node = triple.getObject
          val semanticURItype = if (isURI(node)) {
            SemanticURIGuesser.guessSemanticURIType(node.toString())
          } else
            Future.successful(SemanticURIGuesser.Unknown) // TODO NONE
          semanticURItype .map{ st => (node, st) }
        }
      Future sequence semanticURItypes
      }
    })
    MonadicHelpers.tryToFutureFlat(r).flatMap( identity )
  }
}