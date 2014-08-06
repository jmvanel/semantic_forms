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
      actionURI:String="/save" ) : Elem = {
    val store =  RDFStoreObject.store
//    RDFStoreObject.printGraphList
    // TODO load ontologies from local SPARQL; probably use a pointed graph
    if(blankNode != "true"){
      storeURI(makeUri(uri), store)
      Logger.getRootLogger().info(s"After storeURI(makeUri($uri), store)")
    }
    store.readTransaction {
      val allNamedGraphs = store.getGraph(makeUri("urn:x-arq:UnionGraph"))
      graf2form(allNamedGraphs, uri, hrefPrefix, blankNode, editable, actionURI)
    }
  }

  /** create a form for given URI resource (instance) with background knowledge in given graph */
  def graf2form(graph: Rdf#Graph, uri:String,
      hrefPrefix:String="", blankNode:String="",
      editable:Boolean=false,
      actionURI:String="/save"
  ): Elem = {
    val factory = new FormSyntaxFactory[Rdf](graph)
//    println (Ops.emptyGraph )
//    println("graf2form " + " " + graph.hashCode() + "\n" 
////        + (graph.toIterable).mkString("\n")
//                + factory.printGraph(graph) )
    val form = factory.createForm(
        if( blankNode == "true" )
          /* TDB specific:
           * Jena supports "concrete bnodes" in SPARQL syntax as pseudo URIs in the "_" URI scheme
           * (it's an illegal name for a URI scheme)
          */
          BNode(uri)
//          BNode("_:"+uri)
//            URI("_:"+uri)
          else URI(uri)
        // find properties from instance
//       , Seq( foaf.title, 
//          foaf.givenName,
//          foaf.familyName, 
//          foaf.currentProject,
//          foaf.img,
//          foaf.mbox )
    )
    println("form:\n" + form)
    val htmlForm = generateHTML(form, hrefPrefix, editable, actionURI )
    println(htmlForm)
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
}