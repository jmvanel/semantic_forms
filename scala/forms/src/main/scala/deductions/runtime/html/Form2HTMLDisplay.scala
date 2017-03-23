package deductions.runtime.html

import java.net.URLEncoder

import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Unparsed
//import deductions.runtime.views.ToolsPage
	import deductions.runtime.utils.HTTPrequest

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI] {

	import config._
	import prefixes._

	private[html] def createHTMLiteralReadonlyField(l: formMod#LiteralEntry): NodeSeq =
    <xml:group>
      <div class="">{ Unparsed(toPlainString(l.value)) }</div>
      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  def createHTMLResourceReadonlyField(
      resourceEntry: formMod#ResourceEntry,
      hrefPrefix: String = hrefDisplayPrefix,
      request: HTTPrequest = HTTPrequest()
      ): NodeSeq = {

    import resourceEntry._

    val subjectURIstringValue = value.toString()
    val css = cssForURI(subjectURIstringValue)

    /* provide draggable hyperlinks to form's fields, suitable to drop in social media */
    val hyperlinkToField = {
      val id = urlEncode(resourceEntry.property).replace("%", "-")
      /*  ID and NAME tokens must begin with a letter ([A-Za-z]) and may be followed by any number of letters, 
       *  digits ([0-9]), hyphens ("-"), underscores ("_"), colons (":"), and periods ("."). */
      if( subjectURIstringValue != "" ) {
      <a href={ "#" + id } draggable="true">
      <i class="glyphicon glyphicon-link"></i>
      </a>
      <a id={ id }></a>
      } else NodeSeq.Empty
    }

    val hyperlinkToObjectURI =
      <a href={ createHyperlinkString(hrefPrefix, subjectURIstringValue) }
      class={css}
      title={
        s"""Value ${if (subjectURIstringValue != valueLabel) subjectURIstringValue else ""}
              of type ${type_.toString()}"""
      } draggable="true"
      data-uri-subject={resourceEntry.subject.toString()}
      data-uri-value={resourceEntry.value.toString()}
      data-uri-type={resourceEntry.type_.toString()}
      >{
        valueLabel
      }</a>

    val backLinkButton = (if (subjectURIstringValue.size > 0 && showExpertButtons) {
				val title = s""" Reverse links for "$label" "$value" """
				makeBackLinkButton(subjectURIstringValue, title=title )
      } else NodeSeq.Empty )

    val normalNavigationButton = (if (subjectURIstringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary" href={ subjectURIstringValue } title={ s"Normal HTTP link to $value" }
      draggable="true"><i class="glyphicon glyphicon-share-alt"></i> </a>
    } else NodeSeq.Empty )

    val thumbnail = {
      val thumbnail = resourceEntry.thumbnail
      val imageURL = if (isImage) Some(value)
      else thumbnail
      if (isImage || thumbnail.isDefined){
        <a class="image-popup-vertical-fit" href={ imageURL.get.toString() } title={s"Image of $valueLabel: ${value.toString()}"}>
          <img src={ imageURL.get.toString() } css="sf-thumbnail" height="40" alt={
          s"Image of $valueLabel: ${value.toString()}"
        }/></a>
      }
      else NodeSeq.Empty
      }

      hyperlinkToField ++
      hyperlinkToObjectURI ++
      Text("\n") ++
      backLinkButton ++
      Text("\n") ++
      normalNavigationButton ++
      Text("\n") ++
      makeDrawGraphLink(subjectURIstringValue) ++
      makeDrawGraphLink(subjectURIstringValue,
          toolURLprefix=
            s"https://scenaristeur.github.io/graphe/?endpoint=${request.localSparqlEndpoint}" +
            s"&sujet=",
            toolname="scenaristeur/graphe"
            ,
            imgWidth=6
          ) ++
      Text("\n") ++
      thumbnail
  }



  private[html] def createHTMLBlankNodeReadonlyField(
    r: formMod#BlankNodeEntry,
    hrefPrefix: String = config.hrefDisplayPrefix) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a>

}
