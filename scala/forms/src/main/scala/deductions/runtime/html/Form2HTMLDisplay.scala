package deductions.runtime.html

import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Unparsed

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

    import resourceEntry._

    val stringValue = value.toString()
    val css = cssForURI(stringValue)
    val alternativeText = "Texte alternatif"

    val hyperlinkToObjectURI =
      <a href={ Form2HTML.createHyperlinkString(hrefPrefix, stringValue) }
      class={css}
      title={
        s"""Value ${if (stringValue != valueLabel) stringValue else ""}
              of type ${type_.toString()}"""
      } draggable="true"> {
        valueLabel
      }</a>

    val backLinkButton = (if (stringValue.size > 0 && showExpertButtons) {
				val title = s""" Reverse links for "$label" "$value" """
				makeBackLinkButton(stringValue, title=title )
      } else NodeSeq.Empty )

    val normalNavigationButton = (if (stringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary" href={ stringValue } title={ s"Normal HTTP link to $value" }
      draggable="true"><i class="glyphicon glyphicon-share-alt"></i> </a>
    } else NodeSeq.Empty )

    val thumbnail = {
      val thumbnail = resourceEntry.thumbnail
      val imageURL = if (isImage) Some(value)
      else thumbnail
      if (isImage || thumbnail.isDefined)
        <img src={ imageURL.get.toString() } css="sf-thumbnail" height="40" alt={
          s"Image of $valueLabel: ${value.toString()}"
        }/>
      else NodeSeq.Empty
      }
      hyperlinkToObjectURI ++
      Text("\n") ++
      backLinkButton ++
      Text("\n") ++
      normalNavigationButton ++
      Text("\n") ++
      makeDrawGraphLink(stringValue) ++
      Text("\n") ++
      thumbnail
  }



  def createHTMLBlankNodeReadonlyField(
    r: formMod#BlankNodeEntry,
    hrefPrefix: String) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>

}
