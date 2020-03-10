package deductions.runtime.services.html

import deductions.runtime.abstract_syntax.{FormSyntaxJson, UnfilledFormFactory}
import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.sparql_cache.RDFCacheAlgo
import deductions.runtime.utils.{Configuration, I18NMessages, RDFPrefixes}
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

import scala.util.{Success, Try}
import scala.xml.NodeSeq

import deductions.runtime.utils.CSSClasses
import deductions.runtime.utils.RDFTreeDuplicator
import java.net.URLDecoder

trait CreationFormAlgo[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
with UnfilledFormFactory[Rdf, DATASET]
with HTML5TypesTrait[Rdf]
with RDFPrefixes[Rdf]
with FormSyntaxJson[Rdf]
with RDFTreeDuplicator[Rdf]
with CSSClasses {

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
  def create(classUri: String,
             formSpecURI: String = "", graphURI: String = "",
             request: HTTPrequest ): Try[NodeSeq] = {

    val lang = request.getLanguage()
    val form0 = createData(classUri, formSpecURI, request)
    val form = createPrefillForm(form0, request)

    val rawForm = generateHTML(
      form, hrefPrefix = "",
      editable = true,
      actionURI = actionURI,
      graphURI = graphURI, request = request,
      cssForURI = cssClasses.formFieldCSSClass,
      cssForProperty = cssClasses.formLabelCSSClass)

    val editingHeaders = (
      for (classe <- form.classs)
        yield makeEditingHeader(fromUri(uriNodeToURI(classe)), lang, formSpecURI, graphURI)
    ) . flatten

    Success(
      editingHeaders
        ++
        rawForm)
  }

  /** make raw Data for a form for instance creation; transactions Inside */
  def createData(classUri: String,
                 formSpecURI: String = "",
                 request: HTTPrequest
                 ) : FormSyntax = {
    val classURI = URI(classUri)
    retrieveURIBody(classURI, dataset, request, transactionsInside = true)
    implicit val lang = request.getLanguage()
    implicit val graph: Rdf#Graph = allNamedGraph
    val form = createFormFromClass(classURI, formSpecURI, request)
    form
  }

  def createDataAsJSON(classUri: String,
                       formSpecURI: String = "",
//                       graphURI: String = "",
                       request: HTTPrequest ) = {
    val formSyntax =
//      rdfStore.rw( dataset, {
      createData(classUri, formSpecURI, request)
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
    create(uri, lang, request=HTTPrequest() ).getOrElse(
      <p>Problem occured when creating an XHTML input form from a class URI.</p>)
  }

  /** create Prefilled input Form, from the Referer URL */
  def createPrefillForm(form: FormSyntax, request: HTTPrequest) : FormSyntax = {
    import request._
    val referer = getHTTPheaderValue("Referer") getOrElse("")
    val referenceSubjectURI = URLDecoder.decode( substringAfter( referer, config.hrefDisplayPrefix() ), "UTF-8")
    // Referer example: http://localhost:9000/display?displayuri=http%3A%2F%2F172.17.0.1%3A9000%2Fldp%2FHerv%C3%A9_Mureau
//    println(s""">>>> createPrefillForm: referenceSubjectURI $referenceSubjectURI
//        request.path ${request.path}""")
    if( referenceSubjectURI != "" &&
        path == "/create" &&
        getHTTPparameterValue("prefill").getOrElse("").trim() != "no" )
    wrapInReadTransaction {
      val newTriples = duplicateTree(makeUri(referenceSubjectURI), form.subject, allNamedGraph) . toList
      val newFfields = for (field <- form.fields) yield {
        val found = newTriples.find(triple => triple.predicate == makeURI(field.property))
        found match {
          case Some(t) => field.copyEntry(value = t.objectt)
          case None  => field
        }
      }
      form.fields = newFfields
    }
    form
  }
}
