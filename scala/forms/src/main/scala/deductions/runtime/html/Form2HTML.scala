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
trait Form2HTML[NODE, URI <: NODE] extends FormModule[NODE, URI] {
  type fm = FormModule[NODE, URI]

  def generateHTML(form: fm#FormSyntax[NODE, URI],
    hrefPrefix: String = "",
    editable: Boolean = false,
    actionURI: String = "/save", graphURI: String = ""): Elem = {

    val htmlForm =
      <div class="form">
        <input type="hidden" name="uri" value={ urlEncode(form.subject) }/>
        {
          for (field <- form.fields) yield {
            <div class="form-group">
              <div class="row">
                <label class="control-label" title={ field.comment }>{ field.label }</label>
                <div class="input">
                  {
                    createHTMLField(field, editable, hrefPrefix)
                  }
                </div>
              </div>
            </div>
          }
        }
      </div>

    if (editable)
      <form action={ actionURI } method="POST">
        <p class="text-right">
          <input value="SAVE" type="submit" class="btn btn-primary btn-lg"/>
        </p>
        <input type="hidden" name="url" value={ urlEncode(form.subject) }/>
        <input type="hidden" name="graphURI" value={ urlEncode(graphURI) }/>
        { htmlForm }
        <p class="text-right">
          <input value="SAVE" type="submit" class="btn btn-primary btn-lg pull-right"/>
        </p>
      </form>
    else
      htmlForm
  }

  private def createHTMLField(field: fm#Entry, editable: Boolean,
    hrefPrefix: String = ""): xml.NodeSeq = {
    field match {
      case l: LiteralEntry =>
        {
          if (editable) {
            createHTMLiteralEditableLField(l)
          } else {
            <div>{ l.value }</div>
          }
        }
      case r: ResourceEntry =>
        /* link to a known resource of the right type,
           * or create a sub-form for a blank node of an ancillary type (like a street address),
           * or just create a new resource with its type, given by range, or derived
           * (like in N3Form in EulerGUI ) */
        {
          if (editable) {
            <div>
              <input class="form-control" value={ r.value.toString } name={ makeHTMLId(r) } list={ makeHTMLIdForDatalist(r) } data-type={ r.type_.toString() }/>
              {
                Seq(
                  addDBPediaLookup(r),
                  formatPossibleValues(field),
                  if (r.alreadyInDatabase) {
                    { println("r.alreadyInDatabase " + r) }
                    <input value={ r.value.toString } name={ "ORIG-RES-" + urlEncode(r.property) } type="hidden"/>
                  })
              }
            </div>
          } else
            <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) }>{
              r.valueLabel
            }</a>
        }
      case r: BlankNodeEntry =>
        {
          if (editable) {
            <input class="form-control" value={ r.value.toString } name={ "BLA-" + urlEncode(r.property) } data-type={ r.type_.toString() }/>
            <input value={ r.value.toString } name={ "ORIG-BLA-" + urlEncode(r.property) } type="hidden"/>
          } else
            <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
              r.getId
            }</a>
        }
      case _ => <p>Should not happen! createHTMLField({ field })</p>
    }
  }

  private def makeHTMLId(re: Entry) = {
    "RES-" + urlEncode(re.property)
  }

  /** create HTM Literal Editable Field, taking in account owl:DatatypeProperty's range */
  def createHTMLiteralEditableLField(lit: LiteralEntry): xml.NodeSeq = {
    val elem = lit.type_.toString() match {

      // TODO match graph pattern for interval datatype ; see issue #17
      case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
        (for (n <- Range(0, 6)) yield (
          <input type="radio" name={ "LIT-" + urlEncode(lit.property) } id={ "LIT-" + urlEncode(lit.property) } checked={ if (n.toString.equals(lit.value)) "checked" else null } value={ n.toString }/>
          <label for={ "LIT-" + urlEncode(lit.property) }>{ n }</label>
        )).flatten

      case _ =>
        <input class="form-control" value={ lit.value } name={ "LIT-" + urlEncode(lit.property) }/>
    }
    elem ++
      <input value={ lit.value } name={ "ORIG-LIT-" + urlEncode(lit.property) } type="hidden"/>
  }

  private def makeHTMLIdForDatalist(re: Entry) = {
    "possibleValues-" + (
      re match {
        case re: ResourceEntry => (re.property + "--" + re.value).hashCode().toString()
        case lit: LiteralEntry => (lit.property + "--" + lit.value).hashCode().toString()
        case bn: BlankNodeEntry => (bn.property + "--" + bn.value).hashCode().toString()
      })
  }

  private def formatPossibleValues(field: fm#Entry): Elem = {
    field match {
      case re: ResourceEntry =>
        <datalist id={ makeHTMLIdForDatalist(re) }>
          {
            for (value <- re.possibleValues) yield <option label={ value._2 } value={ value._1.toString() }>
                                                   </option>
          }
        </datalist>
      case _ => <span/>
    }
  }

  def addDBPediaLookup(r: ResourceEntry): Elem = {
    // format: OFF    <-- for scalariform
    if (r.widgetType == DBPediaLookup)
      <script>
        installDbpediaComplete( '{ makeHTMLId(r) }' );
      </script>
    else <div/>
    // format: ON    <-- for scalariform
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
