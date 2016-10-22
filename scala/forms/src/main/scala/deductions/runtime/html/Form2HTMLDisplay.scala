package deductions.runtime.html

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed
import java.net.URLEncoder

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
    val css = cssForURI(stringValue)

    val hyperlinkToObjectURI =
      <a href={ Form2HTML.createHyperlinkString(hrefPrefix, stringValue) }
      class={css}
      title={
        s"""Value ${if (stringValue != r.valueLabel) stringValue else ""}
              of type ${r.type_.toString()}"""
      } draggable="true"> {
        r.valueLabel
      }</a>
    
    val backLinkButton = (if (stringValue.size > 0 && showExpertButtons) {      
				val title = s""" Reverse links for "${r.label}" "${r.value}" """
				makeBackLinkButton(stringValue, title=title )
      } else new Text(""))
      
    val normalNavigationButton = (if (stringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary" href={ stringValue } title={ s"Normal HTTP link to ${r.value}" }
      draggable="true"><i class="glyphicon glyphicon-share-alt"></i> </a>
    } else new Text(""))
    
    Seq(
      hyperlinkToObjectURI,
      Text("  "),
      backLinkButton,
      Text("  "),
      normalNavigationButton,
      Text("  "),
      makeDrawGraphLink(stringValue) )
  }

  def createHTMLBlankNodeReadonlyField(
    r: fm#BlankNodeEntry,
    hrefPrefix: String) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>
            
}