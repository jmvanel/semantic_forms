package deductions.runtime.html

import java.net.{ URLDecoder, URLEncoder }

import deductions.runtime.core.FormModule
import deductions.runtime.utils.{ RDFPrefixesInterface, Timer }
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.Cookie

import org.apache.commons.codec.digest.DigestUtils

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{ Elem, NodeSeq, Text }

import scalaz._
import Scalaz._
import scala.xml.Comment
import scala.xml.Unparsed
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashSet
import scala.xml.EntityRef

/**
 * Abstract Form Syntax to HTML;
 * different modes: display or edit;
 *  takes in account datatype
 */
trait Form2HTML[NODE, URI <: NODE]
  extends Form2HTMLDisplay[NODE, URI]
  with Form2HTMLEdit[NODE, URI]
  with FormModule[NODE, URI]
  with Timer
  with RDFPrefixesInterface {
  self: HTML5Types =>

  import config._

  /**
   * render the given Form Syntax as HTML;
   *  @param hrefPrefix URL prefix pre-pended to created ID's for Hyperlink
   *  @param actionURI, actionURI2 HTML actions for the 2 submit buttons
   *  @param graphURI URI for named graph to save user inputs
   */
  def generateHTML(
    form:       FormModule[NODE, URI]#FormSyntax,
    hrefPrefix: String                           = config.hrefDisplayPrefix,
    editable:   Boolean                          = false,
    actionURI:  String                           = "/save", graphURI: String = "",
    actionURI2: String = "/save",
    request:        HTTPrequest,
    cssForURI:      String      = "",
    cssForProperty: String      = ""): NodeSeq = {

    val htmlFormFields = time(
      "generateHTMLJustFields",
      generateHTMLJustFields(form, hrefPrefix, editable, graphURI, request,
        cssForURI, cssForProperty),
      logger.isInfoEnabled())

    val lang = request.getLanguage

    /* wrap Fields With HTML <form> Tag */
    def wrapFieldsWithFormTag(htmlFormFields: NodeSeq): NodeSeq =
      <form class="sf-standard-form" action={ actionURI } method="POST" id="form">
        <script>
          {
            // Prevent a FORM SUBMIT with ENTER
            Unparsed("""
           document.getElementById("form").onkeypress = function(e) {
             var key = e.charCode || e.keyCode || 0;
             if (key == 13) {
               e.preventDefault();
             }
           }
         """)
          }
        </script>
        {
          if (form.fields.size > 3) // for login form
            addSaveButton(actionURI2)
        }
        { htmlFormFields }
        { addSaveButton(actionURI2) }
      </form>

    def mess(m: String): String = message(m, lang )

    def addSaveButton(actionURIarg: String) =
      if (actionURIarg =/= "")
        <div class="sf-save" zzclass="col col-sm-4 col-sm-offset-4">
          <input value={ mess("SAVE") } formaction={ actionURIarg } type="submit" class="form-control btn btn-primary "/>
        </div>

    //// output begins ////

    languagesInDataStatistics(form, request) ++ (
      if (editable)
        wrapFieldsWithFormTag(htmlFormFields)
      else
        htmlFormFields)
  }

  /**
   * generate HTML, but Just Fields;
   * this lets application developers create their own submit button(s) and <form> tag
   *
   * PENDING if inner functions should need to be overridden, they should NOT be inner
   */
  def generateHTMLJustFields(
    form:       formMod#FormSyntax,
    hrefPrefix: String             = config.hrefDisplayPrefix,
    editable:   Boolean            = false,
    graphURI:   String             = "",
    request:        HTTPrequest,
    cssForURI:      String      = "",
    cssForProperty: String      = ""): NodeSeq = {

    implicit val formImpl: formMod#FormSyntax = form
    val lang = request.getLanguage

    val hiddenInputs: NodeSeq =
      if (editable) {
        <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
        <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
      } else Seq()

    /** make Fields Label And Data for all fields */
    def makeFieldsLabelAndDataTable(
      fields:         Seq[FormEntry],
      cssForURI:      String         = "",
      cssForProperty: String         = ""): NodeSeq = {
      if (!fields.isEmpty) {
        /* mechanism too complex */
        val firstEntry = LiteralEntry()
        for (
          (preceding, field) <- (firstEntry +: fields) zip fields // do not display NullResourceEntry
          if (toPlainString(field.property) =/= "" &&
            (editable ||
              toPlainString(field.value) =/= "" ||
              isSeparator(field)))
        ) yield {
          val dataCSSclass = if (isLanguageDataFittingRequest(field, request))
            ""
          else "sf-data-not-fitting-user-language"
          // println ( s"makeFieldsLabelAndData: ${field.property} dataClass : $dataCSSclass" )
          <div class={
            cssConfig.formLabelAndInputCSSClass + " " +
              dataCSSclass
          }>{
            makeFieldSubject(field) ++
              makeFieldLabel(preceding, field, editable, lang) ++
              createHTMLField(field, editable, hrefPrefix, request, css = cssForURI)
          }</div>
        }
      } else Text("\n")
    }

    /** group by preserving insertion order
     *  See https://stackoverflow.com/questions/9594431/scala-groupby-preserving-insertion-order */
    def groupByOrdered[A, K](t: Traversable[A], f: A => K): scala.collection.mutable.Map[K, LinkedHashSet[A]] = {
      val map = LinkedHashMap[K, LinkedHashSet[A]]().withDefault(_ => LinkedHashSet[A]())
      for (i <- t) {
        val key = f(i)
        map(key) = map(key) + i
      }
      map
    }

    /** make Fields Label And Data for all fields: new layout: Show multiple values in a row */
    def makeFieldsLabelAndData(
      fields:         Seq[FormEntry],
      cssForURI:      String         = "",
      cssForProperty: String         = ""): NodeSeq = {

      val fieldsFiltered = fields.filter(
        field => toPlainString(field.property) =/= "" &&
          (editable ||
            toPlainString(field.value) =/= "" ||
            isSeparator(field)))

      lazy val subject2EntryMap = groupByOrdered(
        fieldsFiltered,
        ((f: FormEntry) => f.subject ))

      val labelsAndData = for ((subject, entries) <- subject2EntryMap) yield {
        makeFieldsLabelAndDataSingleSubject(
          entries.toSeq,
          cssForURI,
          cssForProperty)
      }
      labelsAndData.toSeq.flatten
    }

    /**
     * make Fields Label And Data for all fields, assuming Single Subject
     * layout: Show multiple values in a row
     */
    def makeFieldsLabelAndDataSingleSubject(
      fields:         Seq[FormEntry],
      cssForURI:      String         = "",
      cssForProperty: String         = ""): NodeSeq = {

      lazy val property2EntryMap = groupByOrdered(
        fields,
        ((f: FormEntry) => f.property))

      val labelsAndData = for ((prop, entries) <- property2EntryMap) yield {

        val field = entries.head
        val htmlForEntries =
          for (entry <- entries) yield {
            val htmlField = createHTMLField(
              entry, editable, hrefPrefix, request, css = cssForURI);
            val htmlFieldWithSeparator = htmlField :+
              EntityRef("nbsp") :+
              Text(", ");
            htmlFieldWithSeparator
          }

        <div class="sf-values-group">{
          makeFieldSubject(field) ++
            makeFieldLabelBasic(field, editable, lang) ++
            { htmlForEntries }
        }</div>
      }
      labelsAndData.toSeq
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
        createHTMLField(subjectField, editable, hrefPrefix, css = "",
            request = HTTPrequest(acceptLanguages=Seq(lang)))
      } else NodeSeq.Empty
    }

    //// output begins - generateHTMLJustFields() ////

    val htmlResult: NodeSeq =
      hiddenInputs ++
        <div class={ cssConfig.formRootCSSClass }>
          {
            Comment(s"""Above div wraps second form header and form (form generation traceability)
              class=${cssConfig.formRootCSSClass}""") ++
              <div class="form-group sf-data-header">
                { dataFormHeader(form, lang) }
              </div> ++
              {
                if (request.queryString.contains("tabs")) {
                  makeFieldsGroups()
                } else if (cssConfig.style == "multiple values")
                  makeFieldsLabelAndData(
                    form.fields, cssForURI, cssForProperty)
                else
                  makeFieldsLabelAndDataTable(
                    form.fields, cssForURI, cssForProperty)
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
      <p class="sf-local-rdf-link">{
        <a href={ toPlainString(form.subject) } style="color: rgb(44,133,254);">{ form.title }</a> ++
          (if (form.subject != nullURI)
            <span> &nbsp;&nbsp;&nbsp;= </span> ++
            <span>&lt;<a href={ toPlainString(form.subject) } style="color: rgb(44,133,254);">{ form.subject }</a>&gt;</span>
          else NodeSeq.Empty)
      }</p> ++
        {
          val warningNoTriples =
            if (form.nonEmptyFields().isEmpty &&
              form.subject != nullURI) // case of login form
              <b> No triple for this URI! Click on subjects link above.</b>
            else Text("")
          warningNoTriples
        } ++
        <div class="sf-local-rdf-link">{
          form.formURI match {
            case Some(formURI) if formURI != nullURI =>
              message("Form_specification", lang) + ": " ++
                createHyperlinkElement(toPlainString(formURI), formLabel)
            case _ =>
              Text(s"(automatic form for ${classs.size} classes and data)")
          }
        }</div>
    } else Text("")
  }

  /**
   * create HTML data Field, the value part;
   *  dispatch to various Entry's: LiteralEntry, ResourceEntry, BlankNodeEntry, RDFListEntry,
   * editable or not;
   * this display of a single RDF triple given in field data structure;
   * the property and its label are done elsewhere in makeFieldsLabelAndData();
   * should not need to be overriden
   */
  def createHTMLField(field: formMod#Entry, editable: Boolean,
                      hrefPrefix: String = config.hrefDisplayPrefix,
                      request:        HTTPrequest,
                      displayInTable: Boolean     = false,
                      css:            String      = cssConfig.formFieldCSSClass)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {

    if (isSeparator(field))
      return NodeSeq.Empty

    val isCreateRequest = request.path.contains("create")
    val editableByUser =
      field.metadata === request.userId() ||
        field.metadata === userURI(request)

    // hack instead of true form separator in the form spec in RDF:
    if (field.label.contains("----"))
      return <hr class="sf-separator"/> // Text("----")
    val xmlField = field match {
      case l: formMod#LiteralEntry =>
        if (editable &&
          (editableByUser ||
            isCreateRequest ||
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
          createHTMLResourceEditableField(r, request.getLanguage )
        else
          createHTMLResourceReadonlyField(r, request)

      case r: formMod#BlankNodeEntry =>
        if (editable && (editableByUser || isCreateRequest ||
          toPlainString(field.value) == ""))
          createHTMLBlankNodeEditableField(r)
        else
          createHTMLBlankNodeReadonlyField(r, hrefPrefix)

      case r: formMod#RDFListEntry => <p>RDF List: {
        r.values.fields.map {
          field =>
            createHTMLField(field, editableByUser, request = request)
        }
      }</p>

      case _ =>
        logger.error(
          s"createHTMLField: Should not happen! $field")
        // <p>Should not happen! createHTMLField({ field })</p>
        NodeSeq.Empty
    }

    if (xmlField != (<span/>))
        Seq(createAddRemoveWidgets(field, editable)) ++
          <span class={ if (displayInTable === true) "tooltip-container" else css + " tooltip-container" }>
            { xmlField  ++
              <span class="tooltiptext">
                { makeUserInfoOnTriples(field, request.getLanguage()) }</span>
            }
          </span>
    else <span/>
  }

  private def userURI(request: HTTPrequest): String = {
    makeAbsoluteURIForSaving(request.userId())
  }
}

object Form2HTML {
  def urlEncode(node: Any) = URLEncoder.encode(node.toString, "utf-8")

  /** create URL String for HTML Hyperlink */
  def createHyperlinkString(hrefPrefix: String, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix === "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}
