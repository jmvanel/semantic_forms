package deductions.runtime.html

import scala.xml.Elem
import deductions.runtime.abstract_syntax.FormModule
import java.net.URLEncoder
import Form2HTML._
import scala.xml.NodeSeq
import deductions.runtime.abstract_syntax.DBPediaLookup

/**
 * different modes: display or edit;
 *  take in account datatype
 */
trait Form2HTML[NODE, URI <: NODE]
//    extends FormModule[NODE, URI]
{
  type fm = FormModule[NODE, URI]

  val radioForIntervals = false // TODO the choice should be moved to FormSyntaxFactory

  def toPlainString(n: NODE): String = n.toString()

  /**
   * render the given Form Syntax as HTML;
   *  @param hrefPrefix URL prefix pre-pended to created ID's for Hyperlink
   *  @param actionURI, actionURI2 HTML actions for the 2 submit buttons
   *  @param graphURI URI for named graph to save user inputs
   */
  def generateHTML(form: fm#FormSyntax[NODE, URI],
    hrefPrefix: String = "",
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String = "",
    actionURI2: String = "/save"): NodeSeq = {

    val htmlForm = generateHTMLJustFields(form, hrefPrefix, editable, graphURI)

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

  /**
   * generate HTML, but Just Fields;
   *  this lets application developers create their own submit button(s) and <form> tag
   */
  def generateHTMLJustFields(form: fm#FormSyntax[NODE, URI],
    hrefPrefix: String = "",
    editable: Boolean = false,
    graphURI: String = ""): NodeSeq = {
    val hidden = if (editable) {
      <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
      <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
    } else Seq()
    hidden ++
      <div class="form">
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
        {
          for (field <- form.fields) yield {
            <div class="form-group">
              <div class="row">
                <label class="control-label" title={ field.comment + " - " + field.property }>{ field.label }</label>
                {
                  if (shouldAddAddRemoveWidgets(field, editable))
                    createHTMLField(field, editable, hrefPrefix)
                  else
                    // that's for corporate_risk:
                    <div class="input">
                      { createHTMLField(field, editable, hrefPrefix) }
                    </div>
                }
              </div>
            </div>
          }
        }
      </div>
  }

  private def createHTMLField(field: fm#Entry, editable: Boolean,
    hrefPrefix: String = ""): xml.NodeSeq = {

    val xmlField = field match {
      case l: fm#LiteralEntry =>
        {
          if (editable) {
            createHTMLiteralEditableLField(l)
          } else {
            <div>{ l.value }</div>
          }
        }
      case r: fm#ResourceEntry =>
        /* link to a known resource of the right type,
           * or (TODO) create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
        {
          if (editable) {
            createHTMLResourceEditableLField(r)
          } else
            <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) } title={ "Value of type " + r.type_.toString() }> {
              r.valueLabel
            }</a>
        }
      case r: fm#BlankNodeEntry =>
        {
          if (editable) {
            if (r.openChoice) {
              <input class="form-control" value={ r.value.toString } name={ makeHTMLIdBN(r) } data-type={ r.type_.toString() }>
              </input>
            }
            <input value={ r.value.toString } name={ "ORIG-BLA-" + urlEncode(r.property) } type="hidden">
            </input>
            if (!r.possibleValues.isEmpty)
              <select value={ r.value.toString } name={ makeHTMLIdBN(r) }>
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
  private def createAddRemoveWidgets(field: fm#Entry, editable: Boolean): Elem = {
    if (shouldAddAddRemoveWidgets(field, editable)) {
      // button with an action to duplicate the original HTML widget with (TODO) an empty content
      val widgetName = makeHTMLId(field)
      <input value="+" class="btn-primary" size="1" title={ "Add another value for " + field.label } onClick={
        s""" cloneWidget( "$widgetName" ); """
      }></input>
    } else <span></span>
  }

  private def makeHTMLId(ent: fm#Entry) = ent match {
    case ent: fm#ResourceEntry => makeHTMLIdResource(ent)
    case ent: fm#LiteralEntry => makeHTMLIdForLiteral(ent)
    case ent: fm#BlankNodeEntry => makeHTMLIdBN(ent)
  }
  private def makeHTMLIdResource(re: fm#Entry) = "RES-" + urlEncode(re.property)
  private def makeHTMLIdBN(re: fm#Entry) = "BLA-" + urlEncode(re.property)
  private def makeHTMLIdForLiteral(lit: fm#LiteralEntry) = "LIT-" + urlEncode(lit.property)

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  private def createHTMLResourceEditableLField(r: fm#ResourceEntry): NodeSeq = {
    <div>
      {
        Seq( // format: OFF
        if (r.openChoice)
          <input class="form-control" value={ r.value.toString }
            name={ makeHTMLIdResource(r) }
            list={ makeHTMLIdForDatalist(r) }
            data-type={ r.type_.toString() }
            placeholder={ s"Enter or paste a resource URI, URL, IRI, etc of type ${r.type_.toString()}" }
            onkeyup={if (r.widgetType == DBPediaLookup) "onkeyupComplete(this);" else null}>
          </input> else <span></span> // format: ON
          ,
          if (r.widgetType == DBPediaLookup)
            formatPossibleValues(r, inDatalist = true)
          else <span></span>
        )
      }
      {
        if (!r.possibleValues.isEmpty && r.widgetType != DBPediaLookup)
          <select value={ r.value.toString } name={ makeHTMLIdResource(r) }>
            { formatPossibleValues(r) }
          </select>
      }
      {
        Seq(
          /* if Resource is alreadyInDatabase, send original value to later save 
           * if there is a change */
          if (r.alreadyInDatabase) {
            { println("r.alreadyInDatabase " + r) }
            <input value={ r.value.toString } name={ "ORIG-RES-" + urlEncode(r.property) } type="hidden">
            </input>
          })
      }
    </div>
  }

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  private def createHTMLiteralEditableLField(lit: fm#LiteralEntry): NodeSeq = {
    val placeholder = s"Enter or paste a string of type ${lit.type_.toString()}"
    val elem = lit.type_.toString() match {

      // TODO in FormSyntaxFactory match graph pattern for interval datatype ; see issue #17
      case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        if (radioForIntervals)
          (for (n <- Range(0, 6)) yield (
            <input type="radio" name={ makeHTMLIdForLiteral(lit) } id={ makeHTMLIdForLiteral(lit) } checked={ if (n.toString.equals(lit.value)) "checked" else null } value={ n.toString }>
            </input>
            <label for={ makeHTMLIdForLiteral(lit) }>{ n }</label>
          )).flatten
        else {
          <select name={ makeHTMLIdForLiteral(lit) }>
            { formatPossibleValues(lit) }
          </select>
        }

      case _ =>
        <input class="form-control" value={ lit.value.toString() } name={ makeHTMLIdForLiteral(lit) } type={ HTML5Types.xsd2html5TnputType(lit.type_.toString()) } placeholder={ placeholder }>
        </input>
    }
    elem ++
      <input value={ lit.value.toString() } name={ "ORIG-LIT-" + urlEncode(lit.property) } type="hidden">
      </input>
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
      }>{ toPlainString(values._2) }</option>
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
