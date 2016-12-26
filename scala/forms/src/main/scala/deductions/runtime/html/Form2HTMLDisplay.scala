package deductions.runtime.html

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed
import java.net.URLEncoder
import deductions.runtime.abstract_syntax.FormModule

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI] {

	import config._
  import prefixes._

  def createHTMLiteralReadonlyField(l: formMod#LiteralEntry): NodeSeq =
    <xml:group>
      <div class="form-cell-display">{ Unparsed(toPlainString(l.value)) }</div>
      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  def createHTMLResourceReadonlyField(
      resourceEntry: formMod#ResourceEntry,
      hrefPrefix: String = hrefDisplayPrefix ): NodeSeq = {

    val stringValue = resourceEntry.value.toString()
    val css = cssForURI(stringValue)
    val alternativeText = "Texte alternatif"

    val hyperlinkToObjectURI =
      <a href={ Form2HTML.createHyperlinkString(hrefPrefix, stringValue) }
      class={css}
      title={
        s"""Value ${if (stringValue != resourceEntry.valueLabel) stringValue else ""}
              of type ${resourceEntry.type_.toString()}"""
      } draggable="true"> {
        resourceEntry.valueLabel
      }</a>

    val backLinkButton = (if (stringValue.size > 0 && showExpertButtons) {
				val title = s""" Reverse links for "${resourceEntry.label}" "${resourceEntry.value}" """
				makeBackLinkButton(stringValue, title=title )
      } else new Text(""))

    val normalNavigationButton = (if (stringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary" href={ stringValue } title={ s"Normal HTTP link to ${resourceEntry.value}" }
      draggable="true"><i class="glyphicon glyphicon-share-alt"></i> </a>
    } else new Text(""))

    var HTMLElements = Seq(
      hyperlinkToObjectURI,
      Text("  "),
      backLinkButton,
      Text("  "),
      normalNavigationButton,
      Text("  "),
      makeDrawGraphLink(stringValue) )

    if ( resourceEntry.type_ == foaf("Image") ||
        resourceEntry.property == foaf("img") ||
        resourceEntry.property == foaf("thumbnail") ||
        resourceEntry.property == foaf("depiction")
        ) {
        val imageElement =
            <img
            src={resourceEntry.value.toString()}
            css="sf-thumbnail"
            height="40"
            alt={resourceEntry.value.toString()}
            />
        // TODO for alt= , need to have access to the display label for the triple subject
        HTMLElements = HTMLElements ++ Seq(imageElement, Text(" "))
    }

      HTMLElements
  }



  def createHTMLBlankNodeReadonlyField(
    r: formMod#BlankNodeEntry,
    hrefPrefix: String) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>

}
