package deductions.runtime.html

import java.net.URLEncoder

import org.joda.time.DateTime

import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Unparsed
//import deductions.runtime.views.ToolsPage
	import deductions.runtime.utils.HTTPrequest
	import scala.xml.UnprefixedAttribute

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
    extends Form2HTMLBase[NODE, URI] {

	import config._
	import prefixes._

	private[html] def createHTMLiteralReadonlyField(l: formMod#LiteralEntry): NodeSeq =
    <xml:group>
      <div class={css.cssClasses.formDivInputCSSClass}>{ Unparsed(toPlainString(l.value)) }{makeUserInfoOnTriples(l.metadata,l.timeMetadata)}</div>

      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  def createHTMLResourceReadonlyField(
      resourceEntry: formMod#ResourceEntry,
      hrefPrefix: String = hrefDisplayPrefix,
      request: HTTPrequest = HTTPrequest()
      ): NodeSeq = {

    import resourceEntry._

    val objectURIstringValue = value.toString()
    val css = cssForURI(objectURIstringValue)

    /* provide draggable hyperlinks to form's fields, suitable to drop in social media */
    val hyperlinkToField = {
      val id = urlEncode(resourceEntry.property).replace("%", "-")
      /*  ID and NAME tokens must begin with a letter ([A-Za-z]) and may be followed by any number of letters, 
       *  digits ([0-9]), hyphens ("-"), underscores ("_"), colons (":"), and periods ("."). */
      if( objectURIstringValue != "" ) {
      <a href={ "#" + id } draggable="true">
      <i class="glyphicon glyphicon-link"></i>
      </a>
      <a id={ id }></a>
      } else NodeSeq.Empty
    }

    //    val atts = new UnprefixedAttribute("", )

    val hyperlinkToObjectURI =
      addTripleAttributesToXMLElement(
        <a href={ createHyperlinkString(hrefPrefix, objectURIstringValue) } class={ css } title={
          s"""Value ${if (objectURIstringValue != valueLabel) objectURIstringValue else ""}
              of type ${type_.toString()}"""
        } draggable="true" >{
          valueLabel
        }</a>,
        resourceEntry)

    val backLinkButton = (if (objectURIstringValue.size > 0 && showExpertButtons) {
				val title = s""" Reverse links for "$label" "$value" """
				makeBackLinkButton(objectURIstringValue, title=title )
      } else NodeSeq.Empty )

    val normalNavigationButton = (if (objectURIstringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary" href={ objectURIstringValue } title={ s"Normal HTTP link to $value" }
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
      makeDrawGraphLink(objectURIstringValue) ++
      makeDrawGraphLink(objectURIstringValue,
          toolURLprefix=
            s"https://scenaristeur.github.io/graphe/?endpoint=${request.localSparqlEndpoint}" +
            s"&sujet=",
            toolname="scenaristeur/graphe"
            ,
            imgWidth=6
          ) ++
      Text("\n") ++
      thumbnail ++
      {makeUserInfoOnTriples(resourceEntry.metadata,resourceEntry.timeMetadata)}
  }

  private def makeUserInfoOnTriples(userMetadata: String,timeMetadata: Long) ={
    val time :String = new DateTime(timeMetadata).toDateTime.toString("dd/MM/yyyy HH:mm")
    if (timeMetadata != -1){
      <p>
        modifié par: {userMetadata} le {time}
      </p>
    }
    else <p></p>
  }

  private[html] def createHTMLBlankNodeReadonlyField(
    r: formMod#BlankNodeEntry,
    hrefPrefix: String = config.hrefDisplayPrefix) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
    }</a> ++
        {makeUserInfoOnTriples(r.metadata,r.timeMetadata)}

}
