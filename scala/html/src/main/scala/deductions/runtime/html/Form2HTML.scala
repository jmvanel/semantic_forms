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

import scalaz._
import Scalaz._
import scala.xml.Comment

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
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String = config.hrefDisplayPrefix,
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en",
                   request: HTTPrequest = HTTPrequest(),
                   cssForURI: String = "",
                   cssForProperty: String = "" ): NodeSeq = {

   val htmlFormFields = time("generateHTMLJustFields",
      generateHTMLJustFields(form, hrefPrefix, editable, graphURI, lang, request,
    		  cssForURI, cssForProperty ))

		/* wrap Fields With HTML <form> Tag */
    def wrapFieldsWithFormTag(htmlFormFields: NodeSeq): NodeSeq =

      <form class="sf-standard-form" action={ actionURI } method="POST" id="form">
        { addSaveButton(actionURI2) }
        <br></br>
        { htmlFormFields }
        { addSaveButton(actionURI2) }
      </form>

    def addSaveButton(actionURIarg: String) =
      if (actionURIarg =/= "")
        <div class="col col-sm-4 col-sm-offset-4">
          <input value={ mess("SAVE") } formaction={ actionURIarg }
          type="submit" class="form-control btn btn-primary "/>
        </div>

    def mess(m: String): String = message(m, lang)

    //// output begins ////

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
                             request: HTTPrequest = HTTPrequest(),
                             cssForURI: String = "",
                             cssForProperty: String = "" ): NodeSeq = {

    implicit val formImpl: formMod#FormSyntax = form

    val hiddenInputs: NodeSeq =
      if (editable) {
        <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
        <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
      } else Seq()

    /* make Fields Label And Data */
    def makeFieldsLabelAndData(
      fields:         Seq[FormEntry],
      cssForURI:      String         = "",
      cssForProperty: String         = ""): NodeSeq = {
      if (!fields.isEmpty) {
        val lastEntry = fields.last
        for (
          (preceding, field) <- (lastEntry +: fields) zip fields // do not display NullResourceEntry
          if ( toPlainString(field.property) =/= "")
        ) yield {
          if (editable ||
            toPlainString(field.value) =/= "" ||
            isSeparator(field)) {
            <div class={ css.cssClasses.formLabelAndInputCSSClass }>{
              makeFieldSubject(field) ++
                makeFieldLabel(preceding, field, editable, lang,
                  cssForProperty = cssForProperty) ++
                createHTMLField(field, editable, hrefPrefix, lang, request, css = cssForURI)
            }</div>
          } else Text("\n")
        }
      } else Text("\n")
    }

    /* makeFieldsGroups Builds a groups of HTML fields to be used with the jQuery UI tabs generator
     *
     * TODO extract Fields Groups feature in specific Trait
     * @return NodeSeq Fragment HTML containing a group of fields */
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
        createHTMLField(subjectField, editable, hrefPrefix, lang, css="")
      } else NodeSeq.Empty
    }

    //// output begins ////

    val htmlResult: NodeSeq =
      hiddenInputs ++
        <div class={ css.cssClasses.formRootCSSClass }>
          {
            Comment(s"Above div wraps second form header and form (form generation traceability) class=${css.cssClasses.formRootCSSClass}") ++
              <input type="hidden" name="uri" value={ urlEncode(form.subject) }/> ++
              <div class="form-group">
                <div class="col-xs-12">
                  { dataFormHeader(form, lang) }
                </div>
              </div> ++
              {
                if (request.queryString.contains("tabs")) {
                  makeFieldsGroups()
                } else
                  makeFieldsLabelAndData(
                    form.fields,
                    cssForURI, cssForProperty)
              }
          }
        </div>
    return htmlResult
  }

  /**
   * Form Header inside the form box with data fields;
   * it displays:
   *  - form title
   *  - form subject URI
   *  - class or Form specification
   */
  private def dataFormHeader(form: formMod#FormSyntax, lang: String): NodeSeq = {
    import form._
    if (toPlainString(subject) =/= "") {
      Text(form.title) ++
        (if (form.subject != nullURI)
          Text(", at URI ") ++
          <a href={ toPlainString(form.subject) } style="color: rgb(44,133,254);">&lt;{ form.subject }&gt;</a>
        else NodeSeq.Empty) ++
        {
          val warningNoTriples =
            if (form.nonEmptyFields().isEmpty &&
                form.subject != nullURI) // case of login form
              <b> No triple for this URI! Click on subjects link above.</b>
            else Text("")
          warningNoTriples
        } ++
        <div>{
          form.formURI match {
            case Some(formURI) if formURI != nullURI =>
              I18NMessages.get("Form_specification", lang) + ": " ++
                createHyperlinkElement(toPlainString(formURI), formLabel)
            case _ =>
//            ( for(classe <- classs) yield
//              createHyperlinkElement(toPlainString(classe), toPlainString(classe)) ++
//              " (automatic form)"
//            )
            Text(s"(automatic form for ${classs.size} classes and data)")
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
                              request: HTTPrequest = HTTPrequest(),
                              displayInTable: Boolean = false,
                              css: String="sf-value-block col-xs-12 col-sm-9 col-md-9")(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {

    if( isSeparator(field) )
      return NodeSeq.Empty
//    if( field.property.toString().contains("species"))
//    	println(s"DEBUG !!!!!!!!!! $field")

    val isCreateRequest = request.path.contains("create")
    val editableByUser =
              field.metadata === request.userId() ||
              field.metadata === userURI(request)
//    println(s"DEBUG !!!!!!!!!! editableByUser=$editableByUser <- field.metadata=${field.metadata} =? request.userId()=${request.userId()}")

    // hack instead of true form separator in the form spec in RDF:
    if (field.label.contains("----"))
      return <hr class="sf-separator"/> // Text("----")
    val xmlField = field match {
      case l: formMod#LiteralEntry =>
//        println(s">>>>>>>>>>>>>>>>>>> createHTMLField ${field.value.toString()}")
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) === ""))
          createHTMLiteralEditableField(l, request)
        else
          createHTMLiteralReadonlyField(l, request)

      case r: formMod#ResourceEntry =>
        /* link to a known resource of the right type,
           * or (TODO) create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) === ""))
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

      case _ =>
        logger.error(
          s"createHTMLField: Should not happen! $field")
        // <p>Should not happen! createHTMLField({ field })</p>
        <span/>
    }

    if( xmlField != Seq(<span/>) )
    // TODO if() below seems useless !!!!
    if (displayInTable === true) {
      Seq(createAddRemoveWidgets(field, editable)) ++
      {xmlField}
    }
    else {
      Seq(createAddRemoveWidgets(field, editable)) ++
        // Jeremy M recommended img-rounded from Bootstrap, but not effect
//        <div class="sf-value-block col-xs-12 col-sm-9 col-md-9">      
//        <div class="sf-value-block">
        <span class={css}>
          {xmlField}
        </span>
    }
    else <span/>
  }

  private def userURI(request: HTTPrequest): String = {
    makeAbsoluteURIForSaving(request.userId())
  }

  /** make Field Data (display) Or Input (edit)
   *  TODO: does not do much! */
//  private def makeFieldDataOrInput(field: formMod#Entry, hrefPrefix: String = config.hrefDisplayPrefix,
//                                   editable: Boolean, lang: String = "en",
//                                   request: HTTPrequest = HTTPrequest(),
//                                   css: String="")(implicit form: FormModule[NODE, URI]#FormSyntax) = {
//    createHTMLField(field, editable, hrefPrefix, lang, request, css=css)
//  }

}

object Form2HTML {
  def urlEncode(node: Any) = URLEncoder.encode(node.toString, "utf-8")

  def createHyperlinkString(hrefPrefix: String, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix === "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}
