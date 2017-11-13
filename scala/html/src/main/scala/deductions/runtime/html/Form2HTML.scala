package deductions.runtime.html

import java.net.{URLDecoder, URLEncoder}

import deductions.runtime.core.FormModule
import deductions.runtime.utils.{RDFPrefixesInterface, Timer}
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.Cookie
import deductions.runtime.utils.I18NMessages

import org.apache.commons.codec.digest.DigestUtils

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{Elem, NodeSeq, Text}
import deductions.runtime.utils.I18NMessages

/** Abstract Form Syntax to HTML;
 * different modes: display or edit;
 *  takes in account datatype
 */
 trait Form2HTML[NODE, URI <: NODE]
    extends Form2HTMLDisplay[NODE, URI]
    with Form2HTMLEdit[NODE, URI]
    with FormModule[NODE, URI]
    with Timer
    with RDFPrefixesInterface  {
  self: HTML5Types =>

  import config._

  /**
   * render the given Form Syntax as HTML;
   *  @param hrefPrefix URL prefix pre-pended to created ID's for Hyperlink
   *  @param actionURI, actionURI2 HTML actions for the 2 submit buttons
   *  @param graphURI URI for named graph to save user inputs
   */
//  private[html]
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String = config.hrefDisplayPrefix,
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en",
                   request: HTTPrequest = HTTPrequest()): NodeSeq = {

   val htmlFormFields = time("generateHTMLJustFields",
      generateHTMLJustFields(form, hrefPrefix, editable, graphURI, lang, request))

		/* wrap Fields With HTML <form> Tag */
    def wrapFieldsWithFormTag(htmlFormFields: NodeSeq): NodeSeq =

      <form class="sf-standard-form" action={ actionURI } method="POST" id="form">
      { if( actionURI != "" )
        <div class="row">
          <div class="col col-sm-4 col-sm-offset-4">
            <input value={ mess("SAVE") }
          type="submit" class="form-control btn btn-primary "/></div> <!--class="pull-right"-->
        </div>
      }
        <br></br>

        { htmlFormFields }

      { if( actionURI2 != "" )
        <div class="row">
          <div class="col col-sm-4 col-sm-offset-4">
            <input value={ mess("SAVE") }
              formaction={ actionURI2 } type="submit" class="form-control btn btn-primary "/></div>
              <!--class="pull-right"-->
        </div>
      }
      </form>

    def mess(m: String): String = message(m, lang)

    if (editable)
      wrapFieldsWithFormTag(htmlFormFields)
    else
      htmlFormFields
  }

  /**
   * generate HTML, but Just Fields;
   * this lets application developers create their own submit button(s) and <form> tag
   *  
   * PENDING if inner functions should need to be overridden, they should NOT be inner
   */
  def generateHTMLJustFields(form: formMod#FormSyntax,
                             hrefPrefix: String = config.hrefDisplayPrefix,
                             editable: Boolean = false,
                             graphURI: String = "", lang: String = "en",
                             request: HTTPrequest = HTTPrequest()): NodeSeq = {

    implicit val formImpl: formMod#FormSyntax = form

    val hidden: NodeSeq = if (editable) {
      <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
      <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
    } else Seq()

    /* make Fields Label And Data */
    def makeFieldsLabelAndData(fields: Seq[FormEntry]): NodeSeq = {
      if (!fields.isEmpty) {
        val lastEntry = fields.last
        val fieldsHTML = for (
          (preceding, field) <- (lastEntry +: fields) zip fields // do not display NullResourceEntry
          if (field.property.toString != "")
        ) yield {
          if (editable ||
              toPlainString(field.value) != "" ||
              isSeparator(field))
            <div class={ css.cssClasses.formLabelAndInputCSSClass }>{
              makeFieldSubject(field) ++
                makeFieldLabel(preceding, field, editable, lang) ++
                makeFieldDataOrInput(field, hrefPrefix, editable, lang, request)
            }</div>
          else
            Text("\n")
        }
        fieldsHTML
      } else Text("\n")
    }

    /* makeFieldsGroups Builds a groups of HTML fields to be used with the jQuery UI tabs generator
     *
     * @return NodeSeq Fragment HTML contenant un groupe de champs
     */
    def makeFieldsGroups(): NodeSeq = {
      val map = form.propertiesGroups

      def makeHref(s: String) = DigestUtils.md5Hex(s)

      // http://jqueryui.com/accordion/ or http://jqueryui.com/tabs/
      val tabsNames = <ul>{
        for (pgs <- map) yield {
          val label = pgs.title
          <li><a href={ "#" + makeHref(label) }>{ label }</a></li>
        }
      }</ul>

      val r = for (pgs <- map) yield {
        val label = pgs.title
        println(s"Fields Group $label")
        Seq(
          <div class="sf-fields-group" id={ makeHref(label) }>
            ,
            <div class="sf-fields-group-title">{ label }</div>
            ,
            { makeFieldsLabelAndData(pgs.fields) }
          </div>)
      }
      val tabs: Seq[Elem] = r.flatten.toSeq
      tabs.+:(tabsNames)
    }

    /* make HTML for Field Subject, if different from form subject */
    def makeFieldSubject(field: FormEntry): NodeSeq = {
      if (field.subject != nullURI && field.subject != form.subject) {
        val subjectField =
          // NOTE: over-use of class ResourceEntry to display the subject instead of normally the object triple:
          ResourceEntry(value = field.subject, valueLabel = field.subjectLabel)
        createHTMLField(subjectField, editable, hrefPrefix, lang)
      } else NodeSeq.Empty
    }

    /// output begins ////

    val htmlResult: NodeSeq =
      hidden ++
        <div class={css.cssClasses.formRootCSSClass  } >
          {
              Text("\n") ++
              <input type="hidden" name="uri" value={ urlEncode(form.subject) }/> ++
              <div class="form-group">
                <div class="col-xs-12"> {dataFormHeader(form, lang) }
              </div></div> ++
              {
//                if (request.rawQueryString.contains("tabs=true")) {
//                println(s">>>> queryString ${request.queryString}")
                if (request.queryString.contains("tabs")) {
//                	println(s">>>> makeFieldsGroups")
                  makeFieldsGroups()
                } else
                  makeFieldsLabelAndData(form.fields)
              }
          }
        </div>
    return htmlResult
  }

  /**
   * Form Header inside the form box with data fields: displays:
   *  - form title
   *  - form subject URI
   *  - class or Form specification
   */
  private def dataFormHeader(form: formMod#FormSyntax, lang: String) = {
    import form._
    if (subject != "") {
      Text(form.title) ++
        (if (form.subject != nullURI)
          Text(", at URI ") ++
          <a href={ toPlainString(form.subject) } style="color: rgb(44,133,254);">&lt;{ form.subject }&gt;</a>
        else NodeSeq.Empty) ++
        <div>{
          form.formURI match {
            case Some(formURI) if formURI != nullURI =>
              I18NMessages.get("Form_specification", lang) + ": " ++
                createHyperlinkElement(toPlainString(formURI), formLabel)
            case _ => "Class " ++
            ( for(classe <- classs) yield
              createHyperlinkElement(toPlainString(classe), toPlainString(classe)) ++
              " (automatic form)"
            )
          }
        }</div>
    } else Text("")
  }

  /** create HTML data Field, the value part;
   *  dispatch to various Entry's: LiteralEntry, ResourceEntry, BlankNodeEntry, RDFListEntry,
   * editable or not;
   * should not need to be overriden */
  def createHTMLField(field: formMod#Entry, editable: Boolean,
                              hrefPrefix: String = config.hrefDisplayPrefix, lang: String = "en",
                              request: HTTPrequest = HTTPrequest(), displayInTable: Boolean = false)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {

    if( isSeparator(field) )
      return NodeSeq.Empty

    val isCreateRequest = request.path.contains("create")
    val editableByUser =
              field.metadata == request.userId()

    // hack instead of true form separator in the form spec in RDF:
    if (field.label.contains("----"))
      return <hr class="sf-separator"/> // Text("----")
    val xmlField = field match {
      case l: formMod#LiteralEntry =>
//        println(s">>>>>>>>>>>>>>>>>>> createHTMLField ${field.value.toString()}")
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) == ""))
          createHTMLiteralEditableField(l, request)
        else
          createHTMLiteralReadonlyField(l, request)

      case r: formMod#ResourceEntry =>
        /* link to a known resource of the right type,
           * or (TODO) create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) == ""))
          createHTMLResourceEditableField(r, lang)
        else
          createHTMLResourceReadonlyField(r, hrefPrefix, request)

      case r: formMod#BlankNodeEntry =>
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) == ""))
          createHTMLBlankNodeEditableField(r)
        else
          createHTMLBlankNodeReadonlyField(r, hrefPrefix)

      case r: formMod#RDFListEntry => <p>RDF List: {
        r.values.fields.map {
          field => field.valueLabel
        }.
          mkString(", ")
      }</p>

      case _ => <p>Should not happen! createHTMLField({ field })</p>
    }
    if (displayInTable == true) {
      Seq(createAddRemoveWidgets(field, editable)) ++
      {xmlField}
    }
    else {

      Seq(createAddRemoveWidgets(field, editable)) ++
        // Jeremy M recommended img-rounded from Bootstrap, but not effect
//        <div class="sf-value-block col-xs-12 col-sm-9 col-md-9">
        <div class="sf-value-block">
          {xmlField}
        </div>

    }
  }

  /** make Field Data (display) Or Input (edit)
   *  TODO: does not do much! */
  private def makeFieldDataOrInput(field: formMod#Entry, hrefPrefix: String = config.hrefDisplayPrefix,
                                   editable: Boolean, lang: String = "en",
                                   request: HTTPrequest = HTTPrequest())(implicit form: FormModule[NODE, URI]#FormSyntax) = {

    def doIt = createHTMLField(field, editable, hrefPrefix, lang, request)

    if (shouldAddAddRemoveWidgets(field, editable))
      doIt
    else if (editable)
      // that's for corporate_risk: TODO : simplify <<<<<<<<<<<<<<
      <div >
        { doIt }
      </div>
    else
      doIt
  }

}

object Form2HTML {
  def urlEncode(node: Any) = URLEncoder.encode(node.toString, "utf-8")

  def createHyperlinkString(hrefPrefix: String, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix == "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}
