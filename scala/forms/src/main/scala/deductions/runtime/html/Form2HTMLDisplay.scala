package deductions.runtime.html

import java.net.URLEncoder

import deductions.runtime.abstract_syntax.FormModule
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
      { Unparsed(toPlainString(l.value)) }{makeUserInfoOnTriples(l)}
      <div>{ if (l.lang != "" && l.lang != "No_language") " > " + l.lang }</div>
    </xml:group>

  /** create HTML Resource Readonly Field
    * PENDING if inner val should need to be overridden, they should be directly in trait
    *  */
  def createHTMLResourceReadonlyField(
                                       resourceEntry: formMod#ResourceEntry,
                                       hrefPrefix: String = hrefDisplayPrefix,
                                       request: HTTPrequest = HTTPrequest()
                                     ): NodeSeq = {

    import resourceEntry._

    val objectURIstringValue = value.toString()
    val css = cssForURI(objectURIstringValue)



      hyperlinkToField(resourceEntry) ++
      hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue, valueLabel, type_, resourceEntry) ++
      backLinkButton (resourceEntry) ++
      normalNavigationButton(resourceEntry) ++
      makeDrawGraphLink(objectURIstringValue) ++
      displayThumbnail(resourceEntry) ++
      {makeUserInfoOnTriples(resourceEntry)}
  }

  /** hyperlink To RDF property */
  def hyperlinkToField(resourceEntry: formMod#ResourceEntry
//      , objectURIstringValue: String
      ) = {
    val id = urlEncode(resourceEntry.property).replace("%", "-")
    /*  ID and NAME tokens must begin with a letter ([A-Za-z]) and may be followed by any number of letters,
     *  digits ([0-9]), hyphens ("-"), underscores ("_"), colons (":"), and periods ("."). */

    val objectURIstringValue = resourceEntry.value.toString()
    if( objectURIstringValue != "" ) {
      <a href={ "#" + id } draggable="true">
        <i class="glyphicon glyphicon-link"></i>
      </a>
        <a id={ id }></a>
    } else NodeSeq.Empty
  }

  def hyperlinkToURI(hrefPrefix: String, objectURIstringValue: String, valueLabel: String, type_ : NODE, resourceEntry: formMod#ResourceEntry) = {
    addTripleAttributesToXMLElement(
      <a href={createHyperlinkString(hrefPrefix, objectURIstringValue)} class={cssForURI(objectURIstringValue)} title=
      {s"""Value ${if (objectURIstringValue != valueLabel) objectURIstringValue else ""}
              of type ${type_.toString()}"""} draggable="true">
        {valueLabel}
      </a>,
      resourceEntry)
  }

  private def backLinkButton(resourceEntry: formMod#ResourceEntry) = {
    import resourceEntry._
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 && showExpertButtons) {
      val title = s""" Reverse links for "$label" "$value" """
      makeBackLinkButton(objectURIstringValue, title = title)
    } else NodeSeq.Empty)
  }

  def normalNavigationButton(resourceEntry: formMod#ResourceEntry) = {
    val value = resourceEntry.value
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 && showExpertButtons) {
      <a class="btn btn-primary btn-xs" href={objectURIstringValue} title={s"Normal HTTP link to $value"}
         draggable="true">
        <i class="glyphicon glyphicon-share-alt"></i>
      </a>
    } else NodeSeq.Empty)
  }

  private def displayThumbnail(resourceEntry: formMod#ResourceEntry): NodeSeq = {
    import resourceEntry._
    val imageURL = if (isImage) Some(value)
    else thumbnail
    if (isImage || thumbnail.isDefined) {
      <a class="image-popup-vertical-fit" href={ imageURL.get.toString() } title={ s"Image of $valueLabel: ${value.toString()}" }>
        <img src={ imageURL.get.toString() } css="sf-thumbnail" height="40" alt={ s"Image of $valueLabel: ${value.toString()}" }/>
      </a>
    } else NodeSeq.Empty
  }


  private def makeUserInfoOnTriples(resourceEntry: formMod#Entry) ={
    val userMetadata = resourceEntry.metadata
    val timeMetadata = resourceEntry.timeMetadata
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
      {makeUserInfoOnTriples(r)}

}
