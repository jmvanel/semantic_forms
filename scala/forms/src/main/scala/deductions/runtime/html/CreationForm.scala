package deductions.runtime.html

import scala.util.Try
import scala.xml.NodeSeq

import org.w3.banana.RDF

import deductions.runtime.abstract_syntax.FormSyntaxJson
import deductions.runtime.abstract_syntax.UnfilledFormFactory
import deductions.runtime.services.Configuration
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.utils.I18NMessages
import deductions.runtime.utils.RDFPrefixes
import scala.util.Success

trait CreationFormAlgo[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with UnfilledFormFactory[Rdf, DATASET]
with HTML5TypesTrait[Rdf]
with RDFPrefixes[Rdf]
with FormSyntaxJson[Rdf] {

  val config: Configuration
  val htmlGenerator: HtmlGeneratorInterface[Rdf#Node, Rdf#URI]
  import htmlGenerator._

  import ops._
  /** TODO also defined elsewhere */
  var actionURI = "/save"

  /**
   * create an XHTML input form for a new instance from a class URI;
   *  transactional
   *  TODO classUri should be an Option
   */
  def create(classUri: String, lang: String = "en",
             formSpecURI: String = "", graphURI: String = "", request: HTTPrequest = HTTPrequest()): Try[NodeSeq] = {

    val form = createData(classUri, lang, formSpecURI, request)

    val rawForm = generateHTML(
      form, hrefPrefix = "",
      editable = true,
      actionURI = actionURI,
      lang = lang, graphURI = graphURI, request = request)

    Success(
      Seq(
        makeEditingHeader(fromUri(uriNodeToURI(form.classs)), lang, formSpecURI, graphURI),
        rawForm).flatten)
  }

  /** raw Data for instance creation; transactions Inside */
  def createData(classUri: String, lang0: String = "en",
                 formSpecURI: String = "",
                 request: HTTPrequest
                 ) : FormSyntax = {
    val classURI = URI(classUri)
    retrieveURINoTransaction(classURI, dataset, request, transactionsInside = true)
    implicit val lang = lang0
    implicit val graph: Rdf#Graph = allNamedGraph
    val form = createFormFromClass(classURI, formSpecURI, request)
    form
  }

  def createDataAsJSON(classUri: String, lang: String = "en",
                       formSpecURI: String = "",
//                       graphURI: String = "",
                       request: HTTPrequest = HTTPrequest()) = {
    val formSyntax =
//      rdfStore.rw( dataset, {
      createData(classUri, lang, formSpecURI, request)
//    }) . get
    formSyntax2JSONString(formSyntax)
  }

  /** make form Header about Editing */
  def makeEditingHeader(classUri: String, lang: String,
                        formSpecURI: String, graphURI: String): NodeSeq = {
    <div class="message sf-form-heaer">
      { I18NMessages.get("CREATING", lang) }
      { abbreviateTurtle(classUri) }
    </div>
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
