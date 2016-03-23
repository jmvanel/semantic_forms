package deductions.runtime.html

import scala.util.Try
import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.UnfilledFormFactory
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDFOps
import deductions.runtime.services.Configuration
import deductions.runtime.services.ConfigurationCopy

trait CreationFormAlgo[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with UnfilledFormFactory[Rdf, DATASET]
with HTML5TypesTrait[Rdf]
with Configuration
{
  import ops._
  import rdfStore.transactorSyntax._
  /** TODO also defined elsewhere */
  var actionURI = "/save"

  /**
   * create an XHTML input form for a new instance from a class URI;
   *  transactional
   */
  def create(classUri: String, lang: String = "en",
    formSpecURI: String = "", graphURI: String= "")
      : Try[NodeSeq] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dataset.rw({
      val classURI = URI(classUri)
      retrieveURINoTransaction( classURI, dataset)
      val factory = this
      preferedLanguage = lang
      implicit val graph: Rdf#Graph = allNamedGraph
      val form = factory.createFormFromClass(
        classURI, formSpecURI);
      val ops1 = ops;
      new Form2HTMLBanana[Rdf] with ConfigurationCopy {
        val ops = ops1
        lazy val original:Configuration = CreationFormAlgo.this
      } .
        generateHTML(form, hrefPrefix = "", editable = true, actionURI = actionURI,
            lang=lang, graphURI=graphURI)
    })
  }

  /** create an XHTML input form for a new instance from a class URI; transactional */
  def createElem(uri: String, lang: String = "en")
  (implicit graph: Rdf#Graph)
  : NodeSeq = {
    //	  Await.result(
    create(uri, lang).getOrElse(
      <p>Problem occured when creating an XHTML input form from a class URI.</p>)
    //			  5 seconds )
  }

}
