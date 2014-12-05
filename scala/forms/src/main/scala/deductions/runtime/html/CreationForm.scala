package deductions.runtime.html

import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
import deductions.runtime.sparql_cache.RDFCacheJena
import deductions.runtime.jena.RDFStoreObject
import scala.xml.Elem
import deductions.runtime.abstract_syntax.UnfilledFormFactory
import org.w3.banana.RDFOpsModule

trait CreationForm extends RDFOpsModule
  with Form2HTML[Jena#Node, Jena#URI]
  with RDFCacheJena // TODO depend on generic Rdf
{
  import Ops._
  val nullURI : Rdf#URI = Ops.URI( "" )
  var actionURI = "/save"
  
  /** create an XHTML input form from a class URI */
  def create( uri:String, lang:String="en" )  : Elem = {
    val store =  RDFStoreObject.store
    store.readTransaction {
      val allNamedGraphs = store.getGraph(makeUri("urn:x-arq:UnionGraph"))
      val factory = new UnfilledFormFactory[Rdf](allNamedGraphs, preferedLanguage=lang)
      val form = factory.createFormFromClass(URI(uri))
      println(form)
      val htmlForm = generateHTML(form, hrefPrefix="", editable=true, actionURI )
//    println(htmlForm)
      htmlForm
    }

  }

}