package deductions.runtime.html

import deductions.runtime.utils.RDFPrefixesInterface
import org.joda.time.DateTime

import scala.xml.{NodeSeq, Unparsed}
import deductions.runtime.core.HTTPrequest
import java.net.URLEncoder
import deductions.runtime.core.HTMLutils

import scalaz._
import Scalaz._
import scala.xml.Text
import deductions.runtime.core.FormModule
import deductions.runtime.utils.I18NMessages
import scala.xml.Comment
import deductions.runtime.utils.StringHelpers

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
  extends Form2HTMLBase[NODE, URI]
with FormModule[NODE, URI]
with StringHelpers

  with HTMLutils {

  import config._

  private[html] def createHTMLiteralReadonlyField(
    literalEntry: formMod#LiteralEntry,
    request:      HTTPrequest
  ): NodeSeq = {
    <span>
        {
          val valueDisplayed =
            if (literalEntry.type_.toString().endsWith("dateTime"))
              literalEntry.valueLabel.replaceFirst("T00:00:00$", "")
            else
              literalEntry.valueLabel
          Unparsed(valueDisplayed)
        }
        <span>{ if (literalEntry.lang  =/=  "" && literalEntry.lang  =/=  "No_language") " > " + literalEntry.lang }</span>
      </span>
  }

  /** create HTML Resource Readonly Field
    * PENDING if inner val should need to be overridden, they should be directly in trait
    *  */
  def createHTMLResourceReadonlyField(
                                       resourceEntry: formMod#ResourceEntry,
                                       request: HTTPrequest
                                     ): NodeSeq = {

    import resourceEntry._

    val objectURIstringValue = value.toString()
    val css = cssForURI(objectURIstringValue)

    val typ = firstNODEOrElseEmptyString(type_)

    val details = request.getHTTPparameterValue("details").getOrElse("")

    val widgets = if( details == "") { // default full details
      hyperlinkToField(resourceEntry) ++
      hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue,
          resourceEntry) ++
      displayThumbnail(resourceEntry, request) ++
      backLinkButton(resourceEntry, request) ++
      makeNeighborhoodLink(objectURIstringValue, request) ++
      creationButton(toPlainString(subject),
              type_.map { t => t.toString() },
              request) ++
      expertLinks(resourceEntry, request)

    } else if( details.contains("images") )
        hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue,
          resourceEntry) ++
        displayThumbnail(resourceEntry, request)

      else
        hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue,
          resourceEntry)

      <span class="sf-statistics">{widgets}</span>
  }

  /** expert Links for page header (triples' subject) */
  def expertLinks(uri: String, request: HTTPrequest ): NodeSeq = {
    val resourceEntry = ResourceEntry(value=stringToAbstractURI(uri))
    expertLinks(resourceEntry,request)
  }

  /** expert Links for page header (triples' subject) */
  def expertLinks(
      resourceEntry: formMod#ResourceEntry,
      request: HTTPrequest
      ): NodeSeq = {
    import resourceEntry._
    val uri = value.toString()
    (if (showExpertButtons) {
      <span class="sf-local-rdf-link"> - </span> ++
      showHideExpertButtonsOnClick(
        normalNavigationButton(resourceEntry) ++
        makeDrawGraphLink(uri) ++
        makeDrawGraphLinkSpoggy(uri) ++
        makeDrawGraphLinkLodLive(uri) ++
//        creationButton(uri,
//              type_.map { t => t.toString() },
//              request) ++
        makeClassTableButton(resourceEntry) ++
        hyperlinkForEditingURIinsideForm(uri, request.getLanguage()),
      uri)
    } else NodeSeq.Empty )
  }

  /** create HTML Resource Readonly Field, just hyperlink to URI and thumbnail,
   *  short, no expert stuff
   *  TODO duplication with preceding function createHTMLResourceReadonlyField() */
  def createHTMLResourceReadonlyFieldBriefly(
    resourceEntry: formMod#ResourceEntry,
    request:       HTTPrequest           ): NodeSeq = {
    import resourceEntry._
    val typ = firstNODEOrElseEmptyString(type_)
    val objectURIstringValue = value.toString()
    hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue,
      resourceEntry) ++
      displayThumbnail(resourceEntry, request)
  }

  /** hyperlink To RDF property */
  private def hyperlinkToField(resourceEntry: formMod#ResourceEntry
//      , objectURIstringValue: String
      ) = {
    val id = urlEncode(resourceEntry.property).replace("%", "-")
    /*  ID and NAME tokens must begin with a letter ([A-Za-z]) and may be followed by any number of letters,
     *  digits ([0-9]), hyphens ("-"), underscores ("_"), colons (":"), and periods ("."). */

    val objectURIstringValue = resourceEntry.value.toString()
    if( objectURIstringValue  =/=  "" && id  =/=  "") {
      <a href={ "#" + id } draggable="true" class="sf-local-rdf-link">
        <i class="glyphicon glyphicon-link"></i>
      </a>
        <a id={ id }></a>
    } else NodeSeq.Empty
  }

  private[html] def makeHyperlinkToURI(
    hrefPrefix:     String,
    uriStringValue: String,
    linkText:       String, typ: Seq[NODE]) = {
    val type_ = firstNODEOrElseEmptyString(typ)
    val types0 = typ.mkString(", ")
    val types = if (types0 == "") type_ else types0
    <a href={ createHyperlinkString(hrefPrefix, uriStringValue) } class={ cssForURI(uriStringValue) } title={
      s"""Value ${if (uriStringValue =/= linkText) uriStringValue else ""}
              of type(s) ${types}"""
    } draggable="true">
      { linkText }
    </a>
  }

  private[html] def hyperlinkToURI(hrefPrefix: String, objectURIstringValue: String,
                                   resourceEntry: formMod#ResourceEntry) = {
    addTripleAttributesToXMLElement(
      makeHyperlinkToURI(hrefPrefix, uriStringValue = objectURIstringValue,
        linkText = resourceEntry.valueLabel, typ = resourceEntry.type_),
      resourceEntry)
  }

  private def backLinkButton(resourceEntry: formMod#ResourceEntry,
      request: HTTPrequest): NodeSeq = {
    import resourceEntry._
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 && showExpertButtons) {
      val mess = I18NMessages.get("Reverse-links-reaching", request.getLanguage())
      val title = s""" $mess "$valueLabel" <$value> """
      makeBackLinkButton(objectURIstringValue, title = title, request)
    } else NodeSeq.Empty)
  }

  private def normalNavigationButton(resourceEntry: formMod#ResourceEntry) = {
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 &&
        isDownloadableURL(objectURIstringValue) && showExpertButtons) {
    	val value = resourceEntry.value
      // class="btn btn-primary btn-xs"
      <a class="sf-button btn-primary" href={objectURIstringValue} title={s"Normal HTTP link to $value"}
         draggable="true">
        <i class="glyphicon glyphicon-share-alt"></i>
      </a>
    } else NodeSeq.Empty)
  }

  /** display Thumbnail from triple foaf:img , etc , or self image if URI is an image */
  private def displayThumbnail(resourceEntry: formMod#ResourceEntry,
      request: HTTPrequest): NodeSeq = {
    import resourceEntry._
    val imageURL = if (isImage) Some(value)
    else thumbnail
    if (isImage || thumbnail.isDefined) {
      val title = s"Image of $valueLabel: ${value.toString()}"
      val url = introduceProxyIfnecessary( imageURL.get.toString(), request )
      <a class="image-popup-vertical-fit" href={ imageURL.get.toString() } title={title}>
        <img src={ url }
             css="sf-thumbnail" height="40" alt={title}/>
      </a>
    } else NodeSeq.Empty
  }

  private[html] def createHTMLBlankNodeReadonlyField(
                                                      r: formMod#BlankNodeEntry,
                                                      hrefPrefix: String = config.hrefDisplayPrefix) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
      }</a>

  /** creation Button for a new local URI
   *  either in form header or on rdf:type objects */
  def creationButton(classURIstringValue: String, types: Seq[String],
                     request: HTTPrequest): NodeSeq = {
    val imageURL = "/assets/images/create-instance.svg"
    implicit val _ = request.getLanguage()
    val messCreate_instance = mess("Create_instance_of") + s" <$classURIstringValue>"
//    println(s">>>> creationButton: classURIstringValue: $classURIstringValue, types: $types")
    if ( types.exists { t => t.endsWith("#Class") } ) {
        <a href={
          "/create?uri=" + URLEncoder.encode(classURIstringValue, "UTF-8")
        } title={ messCreate_instance }>
          <img src={ imageURL } css="sf-thumbnail" height="40" alt={ messCreate_instance }/>
        </a>
    } else NodeSeq.Empty
  }

  private def makeClassTableButton(resourceEntry: FormEntry // ResourceEntry
  ): NodeSeq = {
    val classURI = toPlainString(resourceEntry.value)
    val triple =
      if (resourceEntry.isClass) {
        s"?S a <$classURI> ."
      } else
        s"?S ?P1 <$classURI> ."

    val sparlqlQuery = s"""
      CONSTRUCT {
        ?S ?P ?O .
      } WHERE {
        GRAPH ?G {
        ?S ?P ?O .
        $triple
      } }
      """
    val imageURL = "/assets/images/little-table-grid.svg"
    <a href={ "/table?query=" + URLEncoder.encode(sparlqlQuery, "UTF-8") +
        s"&label=<$classURI>"} target="_blank"
       title={s"Table view for <$classURI>"}>
         <img src={ imageURL } css="sf-thumbnail" height="20" alt="Table view">{
           Comment( s" makeClassTableButton URI <${
             classURI.replaceAll(
                 "-[-]+",
                 """\\-\\-""" )
           }> ")
        }</img>
    </a>
  }


  /**
   * Statistics about languages In Data:
   *  - total number of literal triples,
   *  - excluded ones
   */
  def languagesInDataStatistics(
    form:    formMod#FormSyntax,
    request: HTTPrequest): NodeSeq = {
    val fittingCount = countLanguageDataFittingRequest( form, request)
    val literalEntriesCount = getLiteralEntries(form).size

    logger.debug(s"languagesInData: fittingCount $fittingCount, literalEntriesCount $literalEntriesCount")
    // form.fields.collect { case l: formMod#LiteralEntry => l } . foreach ( println(_) )

    // TODO I18N
    if(fittingCount  =/=  literalEntriesCount)
      <span>{literalEntriesCount - fittingCount} data not fitting user language ({request.getLanguage()}) </span>
      <button
        onclick="
console.log(document.getElementsByClassName('sf-data-not-fitting-user-language'));
Array.from(document.getElementsByClassName('sf-data-not-fitting-user-language')).map(
  elem => elem.style.display='block' );"
      >
        Show all
      </button>
    else NodeSeq.Empty
  }

  private def getLiteralEntries(form: formMod#FormSyntax) = {
    form.fields.collect {
//        case l: formMod#LiteralEntry if ( l.valueLabel =/= "" ) => l
        case l: formMod#LiteralEntry if ( toPlainString(l.value) =/= "" ) => l
    }
  }

  private def countLanguageDataFittingRequest(
    form:    formMod#FormSyntax,
    request: HTTPrequest): Int = {
    getLiteralEntries(form).count(
      f => f match {
        case l => isLanguageDataFittingRequest(l, request)
      })
  }

}
