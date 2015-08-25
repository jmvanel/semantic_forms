package deductions.runtime.html

import scala.util.Try
import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.UnfilledFormFactory
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDFOps

trait CreationFormAlgo[Rdf <: RDF, DATASET] extends RDFCacheAlgo[Rdf, DATASET] {
  import ops._
  import rdfStore.transactorSyntax._

  private val nullURI: Rdf#URI = ops.URI("")
  var actionURI = "/save"

  /**
   * create an XHTML input form for a new instance from a class URI;
   *  transactional
   */
  def create(classUri: String, lang: String = "en",
    formSpecURI: String = ""): Try[NodeSeq] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dataset.r({
      val factory = new UnfilledFormFactory[Rdf, DATASET](allNamedGraph, preferedLanguage = lang)
      val form = factory.createFormFromClass(
        URI(classUri),
        formSpecURI);
      //        new Form2HTMLBanana[Rdf] {}.
      new Form2HTML[Rdf#Node, Rdf#URI] {
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
