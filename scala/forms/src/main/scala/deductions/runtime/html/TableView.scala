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

/** Table View of a form (Jena) */
trait TableView extends TableViewModule

/** Table View of a form */
trait TableViewModule
  extends RDFModule
//  with JenaModule
  with Form2HTML[Jena#Node, Jena#URI]
  with RDFCacheJena // TODO depend on generic Rdf
{
  import Ops._
  val nullURI : Rdf#URI = Ops.URI( "" )

  /** create a form for given uri with background knowledge in RDFStoreObject.store  */
  def htmlForm(uri:String, hrefPrefix:String="", blankNode:String="",
      editable:Boolean=false,
      actionURI:String="/save",
      lang:String="en") : Elem = {
    val store =  RDFStoreObject.store
//    RDFStoreObject.printGraphList
    // TODO ? load ontologies from local SPARQL; probably use a pointed graph
    
    if(blankNode != "true"){
      retrieveURI(makeUri(uri), store)
      Logger.getRootLogger().info(s"After storeURI(makeUri($uri), store)")
    }
    store.readTransaction {
      val allNamedGraphs = store.getGraph(makeUri("urn:x-arq:UnionGraph"))
      graf2form(allNamedGraphs, uri, hrefPrefix, blankNode, editable, actionURI, lang)
    }
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
    val f = htmlForm(uri, editable=editable, actionURI=actionURI)
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
    val store = RDFStoreObject.store
    // get the list of ?O such that uri ?P ?O .
    import scala.concurrent.ExecutionContext.Implicits.global
    store.readTransaction {
      val allNamedGraphs = store.getGraph(Ops.makeUri("urn:x-arq:UnionGraph"))
      val triples = Ops.find(allNamedGraphs, Ops.makeUri(uri), ANY, ANY)
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
  }
}