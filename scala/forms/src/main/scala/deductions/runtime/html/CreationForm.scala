package deductions.runtime.html

import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
import deductions.runtime.sparql_cache.RDFCache
import deductions.runtime.jena.RDFStoreObject
import scala.xml.Elem
import deductions.runtime.abstract_syntax.UnfilledFormFactory
import org.w3.banana.RDFOpsModule
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration._
import deductions.runtime.utils.MonadicHelpers

trait CreationForm extends RDFOpsModule
    with Form2HTML[Jena#Node, Jena#URI]
    with RDFCache {
  import ops._
  val nullURI: Rdf#URI = ops.URI("")
  var actionURI = "/save"

  /**
   * create an XHTML input form for a new instance from a class URI;
   *  transactional
   */
  def create(classUri: String, lang: String = "en"): Try[Elem] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val dataset = RDFStoreObject.dataset
    val r = rdfStore.r(dataset, {
      for (
        // TODO use allNamedGraphs from RDFStoreObject
        allNamedGraphs <- rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph"))
      ) yield {
        val factory = new UnfilledFormFactory[Rdf](allNamedGraphs, preferedLanguage = lang)
        val form = factory.createFormFromClass(URI(classUri))
        println(form)
        val htmlForm = generateHTML(form, hrefPrefix = "", editable = true, actionURI)
        htmlForm
      }
    })
    //    MonadicHelpers.tryToFutureFlat(r)
    r.flatMap { identity }
  }

  def createElem(uri: String, lang: String = "en"): Elem = {
    //	  Await.result(
    create(uri, lang).getOrElse(
      <p>Problem occured when creating an XHTML input form from a class URI.</p>)
    //			  5 seconds )
  }

}