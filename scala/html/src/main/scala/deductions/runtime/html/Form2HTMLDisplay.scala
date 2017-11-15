package deductions.runtime.html

import deductions.runtime.utils.RDFPrefixesInterface
import org.joda.time.DateTime

import scala.xml.{NodeSeq, Unparsed}
import deductions.runtime.core.HTTPrequest
import java.net.URLEncoder

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
  extends Form2HTMLBase[NODE, URI]
  with RDFPrefixesInterface {

  import config._


  private[html] def createHTMLiteralReadonlyField(
      l: formMod#LiteralEntry,
      request: HTTPrequest = HTTPrequest()): NodeSeq =
    <xml:group>
      {
        val valueDisplayed =
          if (l.type_.toString().endsWith("dateTime"))
            l.valueLabel.replaceFirst("T00:00:00$", "")
          else
            l.valueLabel
        Unparsed(valueDisplayed)
      }
    	{makeUserInfoOnTriples(l, request.getLanguage())}
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

    val typ = firstNODEOrElseEmptyString(type_)
//  println(s"==== createHTMLResourceReadonlyField: typ: $typ")
    val widgets =
      hyperlinkToField(resourceEntry) ++
      hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue, valueLabel,
          typ,
          resourceEntry) ++
      backLinkButton (resourceEntry) ++
      normalNavigationButton(resourceEntry) ++
      makeDrawGraphLink(objectURIstringValue) ++
      displayThumbnail(resourceEntry) ++
      {makeUserInfoOnTriples(resourceEntry, request.getLanguage())} ++
      creationButton(objectURIstringValue, typ) // TODO pass type_ 
      <span class="sf-statistics">{widgets}</span>
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

  def hyperlinkToURI(hrefPrefix: String, objectURIstringValue: String, valueLabel: String,
      type_ : String, resourceEntry: formMod#ResourceEntry) = {
    addTripleAttributesToXMLElement(
      <a href={createHyperlinkString(hrefPrefix, objectURIstringValue)} class={cssForURI(objectURIstringValue)} title=
      {s"""Value ${if (objectURIstringValue != valueLabel) objectURIstringValue else ""}
              of type ${type_}"""} draggable="true">
        {valueLabel}
      </a>,
      resourceEntry)
  }

  private def backLinkButton(resourceEntry: formMod#ResourceEntry) = {
    import resourceEntry._
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 && showExpertButtons) {
      val title = s""" Reverse links reaching "$valueLabel" <$value> """
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

  private[html] def createHTMLBlankNodeReadonlyField(
                                                      r: formMod#BlankNodeEntry,
                                                      hrefPrefix: String = config.hrefDisplayPrefix) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
      }</a> ++
      {makeUserInfoOnTriples(r)}

  def creationButton(objectURIstringValue: String, typ: String): NodeSeq = {
    val imageURL = "/assets/images/create-instance.svg"
    val mess = s"Create instance of <$objectURIstringValue>"
    if (typ.endsWith("#Class")) {
//      println(s"==== creationButton: typ: $typ")
        <a href={
          "/create?uri=" + URLEncoder.encode(objectURIstringValue, "UTF-8")
        } title={ mess }>
          <img src={ imageURL } css="sf-thumbnail" height="40" alt={ mess }/>
        </a>
    } else NodeSeq.Empty
  }
}
