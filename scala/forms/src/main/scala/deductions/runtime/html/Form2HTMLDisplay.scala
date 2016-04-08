package deductions.runtime.html

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI] {

  def createHTMLiteralReadonlyField(l: fm#LiteralEntry): NodeSeq =
    <xml:group>
      <div class="form-cell-display">{ Unparsed(toPlainString(l.value)) }</div>
      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  def createHTMLResourceReadonlyField(
      r: fm#ResourceEntry,
      hrefPrefix: String): NodeSeq = {
    val stringValue = r.value.toString()
    val normalNavigationButton = if (stringValue == "")
      Text("")
    else
      <a href={ stringValue } title={ s"Normal HTTP link to ${r.value}" }
      draggable="true">LINK</a>
    Seq(
      <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString) }
      title={
        s"""Value ${if (r.value.toString != r.valueLabel) r.value.toString else ""}
              of type ${r.type_.toString()}"""
      } draggable="true"> {
        r.valueLabel
      }</a>,
      Text(" "),

      (if (r.value.toString().size > 0) {
				val title = s""" Reverse links for "${r.label}" "${r.value}" """
        <button type="button" class="btn-primary" readonly="yes"
				title={title}
        data-value={ r.value.toString }
        onClick={ s"backlinks( '${r.value}' )" }
        id={ "BACK-" + r.value }>
        ? --> o
        </button>
      } else new Text("")), normalNavigationButton)
  }

  def createHTMLBlankNodeReadonlyField(
    r: fm#BlankNodeEntry,
    hrefPrefix: String) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>
            
}