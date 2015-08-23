package deductions.runtime.html

import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.XML
import java.net.URLEncoder
import Form2HTML._
import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.abstract_syntax.DBPediaLookup
import deductions.runtime.utils.Timer

/**
 * different modes: display or edit;
 *  takes in account datatype
 */
trait Form2HTML[NODE, URI <: NODE]
    extends Timer // TODO: extends HTML5TypesTrait
    {
  import HTML5Types._
  type fm = FormModule[NODE, URI]

  val radioForIntervals = false // TODO the choice should be moved to FormSyntaxFactory
  val inputSize = 90

  //  def toPlainString[NODE](n: NODE): String = n.toString()
  def toPlainString(n: NODE): String = n.toString()

  /**
   * render the given Form Syntax as HTML;
   *  @param hrefPrefix URL prefix pre-pended to created ID's for Hyperlink
   *  @param actionURI, actionURI2 HTML actions for the 2 submit buttons
   *  @param graphURI URI for named graph to save user inputs
   */
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
    hrefPrefix: String = "",
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String = "",
    actionURI2: String = "/save"): NodeSeq = {

    val htmlForm = time("generateHTMLJustFields",
      generateHTMLJustFields(form, hrefPrefix, editable, graphURI))

    if (editable)
      <form action={ actionURI } method="POST">
        <p class="text-right">
          <input value="SAVE" type="submit" class="btn btn-primary btn-lg"/>
        </p>
        { htmlForm }
        <p class="text-right">
          <input value="SAVE" type="submit" formaction={ actionURI2 } class="btn btn-primary btn-lg pull-right"/>
        </p>
      </form>
    else
      htmlForm
  }

  /** default is bootstrap classes */
  case class CSSClasses(
    val formRootCSSClass: String = "form",
    val formFieldCSSClass: String = "form-group",
    val formLabelAndInputCSSClass: String = "row",
    val formLabelCSSClass: String = "control-label",
    val formInputCSSClass: String = "input")

  val tableCSSClasses = CSSClasses(
    formRootCSSClass = "form-root",
    formFieldCSSClass = "",
    formLabelAndInputCSSClass = "form-row",
    formLabelCSSClass = "form-label",
    formInputCSSClass = "form-input")

  val localCSS =
    <style type='text/css'>
      .form-row{{ display: table-row; }}
                   .form-cell{{ display: table-cell; }}
                   .form-input{{ display: table-cell; width: 500px; }}
                   .button-add{{ width: 25px; }}
                   .form-label{{ display: table-cell; width: 160px; }}
    </style>

  val localJS =
    <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/pen.js"></script> ++
      <script type="text/javascript" async="true" src="https://rawgit.com/sofish/pen/master/src/markdown.js"></script> ++
      <script type="text/javascript" async="true">
        // function backlinks(uri) {{ }}
        function launchEditorWindow(elem) {{
  var popupWindow = window.open('', 'Edit Markdown text for semantic_forms',
    'height=300, width=300');
  var options = {{
    editor: popupWindow.document.body,
    class: 'pen',
    list: // editor menu list
    [ 'insertimage', 'blockquote', 'h2', 'h3', 'p', 'code', 'insertorderedlist', 'insertunorderedlist', 'inserthorizontalrule',
      'indent', 'outdent', 'bold', 'italic', 'underline', 'createlink' ]
  }}
  popupWindow.document.body.innerHTML = elem.value
  var editor = new Pen( options );
  popupWindow.onbeforeunload = function() {{
    elem.value = editor.toMd(); // return a markdown string
    return void(0)
  }};
        }}
      </script>
  val cssClasses = tableCSSClasses

  /**
   * generate HTML, but Just Fields;
   *  this lets application developers create their own submit button(s) and <form> tag
   */
  def generateHTMLJustFields(form: FormModule[NODE, URI]#FormSyntax,
    hrefPrefix: String = "",
    editable: Boolean = false,
    graphURI: String = ""): NodeSeq = {

    implicit val formImpl: FormModule[NODE, URI]#FormSyntax = form

    val hidden = if (editable) {
      <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
      <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
    } else Seq()
    hidden ++
      <div class={ cssClasses.formRootCSSClass }>
        {
          localCSS ++
            Text("\n") ++
            localJS ++
            Text("\n")
        }
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
        {
          val fields = form.fields
          if (!fields.isEmpty) {
            val lastEntry = fields.last
            for ((preceding, field) <- (lastEntry +: fields) zip fields) yield {

              <div class={ cssClasses.formLabelAndInputCSSClass }>
                {
                  //                  time(s"makeFieldLabel( ${field.label})",
                  makeFieldLabel(preceding, field)
                }
                {
                  //                  time(s"makeFieldInput( ${field.label})",
                  makeFieldInput(field, hrefPrefix, editable)
                }
              </div>
            }
          }
        }
      </div>
  }

  private def makeFieldLabel(preceding: fm#Entry, field: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = {
    // display field label only if different from preceding
    if (preceding.label != field.label)
      <label class={ cssClasses.formLabelCSSClass } title={
        field.comment + " - " + field.property
      } for={ makeHTMLIdResource(field) }>{
        val label = field.label
        // hack before real separators
        if (label.contains("----"))
          label.substring(1).replaceAll("-(-)+", "")
        else label
      }</label>
    else
      <label class={ cssClasses.formLabelCSSClass } title={
        field.comment + " - " + field.property
      }> -- </label>
  }

  private def makeFieldInput(field: fm#Entry, hrefPrefix: String,
    editable: Boolean)(implicit form: FormModule[NODE, URI]#FormSyntax) = {
    if (shouldAddAddRemoveWidgets(field, editable))
      createHTMLField(field, editable, hrefPrefix)
    else
      // that's for corporate_risk:
      <div class={ cssClasses.formInputCSSClass }>
        { createHTMLField(field, editable, hrefPrefix) }
      </div>
  }

  private def createHTMLField(field: fm#Entry, editable: Boolean,
    hrefPrefix: String = "")(implicit form: FormModule[NODE, URI]#FormSyntax): xml.NodeSeq = {

    // hack instead of true form separator:
    if (field.label.contains("----"))
      return <hr style="background:#F87431; border:0; height:4px"/> // Text("----")

    val xmlField = field match {
      case l: fm#LiteralEntry =>
        {
          if (editable) {
            createHTMLiteralEditableLField(l)
          } else {
            <div>{ scala.xml.Unparsed(toPlainString(l.value)) }</div>
          }
        }
      case r: fm#ResourceEntry =>
        /* link to a known resource of the right type,
           * or (TODO) create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
        {
          if (editable) {
            createHTMLResourceEditableField(r)
          } else {
            val stringValue = r.value.toString()
            val normalNavigationButton = if (stringValue == "")
              Text("")
            else
              <a href={ stringValue } title={ s"Normal HTTP link to ${r.value}" }> LINK</a>
        	  // format: OFF
            Seq(
              <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) }
              title={ s"""Value ${if(r.value.toString != r.valueLabel) r.value.toString else ""}
              of type ${r.type_.toString()}""" }> {
                r.valueLabel
              }</a> ,
              Text(" "),

              (if( field.value.toString().size > 0 ) {
            	  <button type="button"
            	  class="btn-primary" readonly="yes"
            	  title={ "Reverse links for " + field.label + " " + field.value} 
            	  data-value={ r.value.toString }
            	  onClick={ s"backlinks('${r.value}')" } 
                id={ "BACK-" + r.value }>? --> o</button>
              } else new Text("") )

              , normalNavigationButton
            )
          }
          // format: ON
        }
      case r: fm#BlankNodeEntry =>
        {
          if (editable) {
            if (r.openChoice) {
              <input class={ cssClasses.formInputCSSClass } value={
                r.value.toString
              } name={ makeHTMLIdBN(r) } data-type={
                r.type_.toString()
              } size={ inputSize.toString() }>
              </input>
            }
            if (!r.possibleValues.isEmpty)
              <select value={ r.valueLabel } name={ makeHTMLIdBN(r) }>
                { formatPossibleValues(r) }
              </select>
            else Seq()

          } else
            <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
              r.getId
            }</a>
        }
      case _ => <p>Should not happen! createHTMLField({ field })</p>
    }

    Seq(createAddRemoveWidgets(field, editable)) ++ xmlField
  }

  private def shouldAddAddRemoveWidgets(field: fm#Entry, editable: Boolean): Boolean = {
    editable && (field.defaults.multivalue && field.openChoice)
  }
  private def createAddRemoveWidgets(field: fm#Entry, editable: Boolean)(implicit form: FormModule[NODE, URI]#FormSyntax): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {
      // button with an action to duplicate the original HTML widget with an empty content
      val widgetName = makeHTMLId(field)
      <input value="+" class="button-add btn-primary" readonly="yes" size="1" title={ "Add another value for " + field.label } onClick={
        s""" cloneWidget( "$widgetName" ); """
      }></input>
    } else <span></span>
  }

  private def makeHTMLId(ent: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = {
    val rawResult = {
      def makeTTLURI(s: NODE) = s"<$s>"
      def makeTTLAnyTerm(value: NODE, ent: fm#Entry) = {
        ent match {
          case lit: fm#LiteralEntry => value
          case _ => makeTTLURI(value)
        }
      }
      makeTTLURI(form.subject) + " " +
        makeTTLURI(ent.property) + " " +
        makeTTLAnyTerm(ent.value, ent) + " .\n"
    }
    urlEncode(rawResult)
  }
  private def makeHTMLIdResource(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLId(re)
  private def makeHTMLIdBN(re: fm#Entry)(implicit form: FormModule[NODE, URI]#FormSyntax) = makeHTMLId(re)
  private def makeHTMLIdForLiteral(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax) =
    makeHTMLId(lit)

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  private def createHTMLResourceEditableField(r: fm#ResourceEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val lookup = r.widgetType == DBPediaLookup
    Seq(
      Text("\n"),
    		// format: OFF
        if (r.openChoice)
          <input class={ cssClasses.formInputCSSClass } value={ r.value.toString }
            name={ makeHTMLIdResource(r) }
            list={ makeHTMLIdForDatalist(r) }
            data-type={ r.type_.toString() }
            placeholder={ if (lookup)
              s"Enter a word; completion with Wikipedia lookup"
              else
              s"Enter or paste a resource URI of type ${r.type_.toString()}" }
            onkeyup={if (lookup) "onkeyupComplete(this);" else null}
            size={inputSize.toString()} >
          </input> else new Text("") // format: ON
          ,
      if (r.widgetType == DBPediaLookup)
        formatPossibleValues(r, inDatalist = true)
      else new Text(""),

      if (!r.possibleValues.isEmpty && r.widgetType != DBPediaLookup)
        <select value={ r.value.toString } name={ makeHTMLIdResource(r) }>
          { formatPossibleValues(r) }
        </select>
      else new Text("\n")
    ).flatMap { identity }
  }

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  private def createHTMLiteralEditableLField(lit: fm#LiteralEntry)(implicit form: FormModule[NODE, URI]#FormSyntax): NodeSeq = {
    val placeholder = s"Enter or paste a string of type ${lit.type_.toString()}"

    val elem = lit.type_.toString() match {

      // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
      case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type="radio" name={ makeHTMLIdForLiteral(lit) } id={ makeHTMLIdForLiteral(lit) } checked={
              if (n.toString.equals(lit.value)) "checked" else null
            } value={ n.toString }>
            </input>
            <label for={ makeHTMLIdForLiteral(lit) }>{ n }</label>
          )).flatten
        else {
          <select name={ makeHTMLIdForLiteral(lit) }>
            { formatPossibleValues(lit) }
          </select>
        }

      case _ =>
        <input class={ cssClasses.formInputCSSClass } value={
          toPlainString(lit.value)
        } name={ makeHTMLIdForLiteral(lit) } type={
          xsd2html5TnputType(lit.type_.toString())
        } placeholder={ placeholder } size={
          inputSize.toString()
        } ondblclick="launchEditorWindow(this);" title="Double click to edit text in popup window as Markdown text">
        </input>
    }
    Text("\n") ++ elem
  }

  private def makeHTMLIdForDatalist(re: fm#Entry) = {
    "possibleValues-" + (
      re match {
        case re: fm#ResourceEntry => (re.property + "--" + re.value).hashCode().toString()
        case lit: fm#LiteralEntry => (lit.property + "--" + lit.value).hashCode().toString()
        case bn: fm#BlankNodeEntry => (bn.property + "--" + bn.value).hashCode().toString()
      })
  }

  /** @return a list of option tags or a datalist tag (with the option tags inside) */
  private def formatPossibleValues(field: fm#Entry, inDatalist: Boolean = false): NodeSeq = {
    def makeHTMLOption(values: (NODE, NODE), field: fm#Entry): Elem = {
      <option value={ toPlainString(values._1) } selected={
        if (field.value.toString() ==
          toPlainString(values._1)) "selected" else null
      } title={ toPlainString(values._1) } label={ toPlainString(values._2) }>{ toPlainString(values._2) }</option>
    }
    val options = Seq(<option value=""></option>) ++
      (for (value <- field.possibleValues) yield makeHTMLOption(value, field))
    if (inDatalist)
      <datalist id={ makeHTMLIdForDatalist(field) }>
        { options }
      </datalist>
    else options
  }
}

object Form2HTML {
  def urlEncode(node: Any) = { URLEncoder.encode(node.toString, "utf-8") }

  def createHyperlinkString(hrefPrefix: String, uri: String, blanknode: Boolean = false): String = {
    if (hrefPrefix == "")
      uri
    else {
      val suffix = if (blanknode) "&blanknode=true" else ""
      hrefPrefix + urlEncode(uri) + suffix
    }
  }
}
