package deductions.runtime.html

import org.w3.banana.RDFModule
import org.w3.banana.jena.Jena
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
import scala.xml.NodeSeq
import org.w3.banana.SparqlGraphModule
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDF
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.abstract_syntax.FormModule

trait CreationForm extends CreationFormAlgo[Jena, Dataset]

trait CreationFormAlgo[Rdf <: RDF, DATASET] extends RDFOpsModule
    with SparqlGraphModule
    with RDFCacheAlgo[Rdf, DATASET]
    with RDFStoreLocalProvider[Rdf, DATASET] {
  import ops._
  import rdfStore.transactorSyntax._

  private val nullURI: Rdf#URI = ops.URI("")
  var actionURI = "/save"

  /**
   * create an XHTML input form for a new instance from a class URI;
   *  transactional
   */
  def create(classUri: String, lang: String = "en"): Try[NodeSeq] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dataset.r({
      val factory = new UnfilledFormFactory[Rdf, DATASET](allNamedGraph, preferedLanguage = lang)
      val form = factory.createFormFromClass(URI(classUri))
      
      // TODO code duplicated in trait TableViewModule.graf2form() 
      new Form2HTML[Rdf#Node, Rdf#URI] {
        import ops._
        override def toPlainString(n: Rdf#Node): String =
          foldNode(n)(fromUri(_), fromBNode(_), fromLiteral(_)._1)
      }.
        generateHTML(form, hrefPrefix = "", editable = true, actionURI = actionURI)
    })
  }

  def createElem(uri: String, lang: String = "en"): NodeSeq = {
    //	  Await.result(
    create(uri, lang).getOrElse(
      <p>Problem occured when creating an XHTML input form from a class URI.</p>)
    //			  5 seconds )
  }

}