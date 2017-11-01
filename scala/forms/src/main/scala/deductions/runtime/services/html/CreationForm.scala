package deductions.runtime.services.html

import deductions.runtime.abstract_syntax.{FormSyntaxJson, UnfilledFormFactory}
import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.{Configuration, I18NMessages, RDFPrefixes}
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

import scala.util.{Success, Try}
import scala.xml.NodeSeq

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

    val editingHeaders = (
      for (classe <- form.classs)
        yield makeEditingHeader(fromUri(uriNodeToURI(classe)), lang, formSpecURI, graphURI)
    ) . flatten

    Success(
      editingHeaders
        ++
        rawForm)
  }

  /** raw Data for instance creation; transactions Inside */
  def createData(classUri: String, lang0: String = "en",
                 formSpecURI: String = "",
                 request: HTTPrequest
                 ) : FormSyntax = {
    val classURI = URI(classUri)
    retrieveURIBody(classURI, dataset, request, transactionsInside = true)
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

  /** make form Header about Editing: "CREATING <name of object>" */
  def makeEditingHeader(classUri: String, lang: String,
                        formSpecURI: String, graphURI: String): NodeSeq = {
    <div class="message sf-form-header">
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
