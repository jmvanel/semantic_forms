package deductions.runtime.html

import java.net.URLEncoder

import deductions.runtime.utils.RDFPrefixesInterface

import scala.xml.Elem
import deductions.runtime.utils.URIHelpers
import scala.xml.NodeSeq
import deductions.runtime.utils.Configuration
import deductions.runtime.core.HTTPrequest

import scalaz._
import Scalaz._
import deductions.runtime.utils.URIManagement
import deductions.runtime.utils.I18NMessages
import deductions.runtime.core.FormModule

/** Basic Widgets with no access to the FormSyntax:
 *  GUI integration: RDFviewer, VOWL, ... */
trait BasicWidgets[NODE, URI <: NODE]
  extends RDFPrefixesInterface
  with URIHelpers
  with URIManagement
  with FormModule[NODE, URI] {
  // TODO why was it added, since we already inherit FormModule ?
  type formMod = FormModule[NODE, URI]

  /** TODO LIMIT should *not* be hardcoded */
  val defaultSPARQLlimit = 100

    val config: Configuration
    import config._

  def hyperlinkForEditingURI(uri: String, lang: String): NodeSeq = {
    implicit val _ = lang
    val hrefEdit = hrefEditPrefix + urlEncode(uri)
    <a class="btn btn-primary btn-xs" href={ hrefEdit } title={ mess("edit_URI") }>
      <i class="glyphicon glyphicon-edit"></i>
    </a>
  }

  private def urlEncode(uri: String) = URLEncoder.encode(uri, "utf-8")

  def hyperlinkForEditingURIinsideForm(uri: String, lang: String): NodeSeq = {
    if( ! uri.startsWith("http://dbpedia.org/resource/"))
      hyperlinkForEditingURI(uri, lang)
    else NodeSeq.Empty
  }

  def hyperlinkForDisplayingURI(uri: String, lang: String): NodeSeq = {
    implicit val _ = lang
    val hrefDisplay = hrefDisplayPrefix() + urlEncode(uri) + "#subject"
    println(s">>>>>>>>>>> linkToShow: $hrefDisplay")
    <a class="btn btn-warning btn-xs" href={ hrefDisplay } title={ mess("display_URI") }>
      <i class="glyphicon"></i>
    </a>
  }

  /** TODO [Marco] settle on a good name for the feature
back links, reverse links or incoming links
don't mix them */
  def makeBackLinkButton(uri: String, title: String = "",
                         request: HTTPrequest,
                         resourceEntry: formMod#ResourceEntry = NullResourceEntry): NodeSeq = {
    val tit = if (title === "")
      I18NMessages.get("Reverse-links-reaching", request.getLanguage()) +
        s"<$uri>;" else title
      val propertyURIEncoded = urlEncode(toPlainString(resourceEntry.property))
      <a href={s"/backlinks?q=${urlEncode(uri)}&q=$propertyURIEncoded"}
        class="sf-button sf-navigation-button"
        title={ tit }
        data-value={s"$uri"} >
        <img src="/assets/images/Back-Link-Icon.svg" width="32" height="32"
             alt="Back-Link" />
      </a>
  }

  def makeBackLinkButtonWithCheckbox(uri: String, title: String = "",
                                     request: HTTPrequest): NodeSeq = {
    val tit = if (title === "")
      I18NMessages.get("Reverse-links-reaching", request.getLanguage()) +
        s"<$uri>;" else title
    <form action="/backlinks" style='display:inline;' >
      <input type="hidden" name="q" value={ uri } />
      <input type="image" alt="Submit" class="sf-button"
        title={ tit }
        data-value={s"$uri"}
        src="/assets/images/Back-Link-Icon.svg" width="32" height="32"
        border="0" >
      </input>
      <input type="checkbox" name="load-uris" title="load the bunch of backlinks URI's" style="height: 7px" />
    </form>
  }

  /** make Link for Drawing Graph; default values are for RDF Viewer */
  def makeDrawGraphLink( uri: String,
      toolURLprefix: String="/assets/rdfviewer/rdfviewer.html?url=",
      toolname: String="RDF Viewer",
      imgWidth:Int=15): NodeSeq = {

    // TODO different link when we are on localhost (transmit RDF String then) or in production (or use N3.js
    // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
    val link = /*hrefDownloadPrefix + */ urlEncode( uri)

    if( isDownloadableURL(uri) &&
        // TODO when we will import RDFa , be more specific here
        // TODO check HTTP Content-type
        // TODO database content <uri> ?P ?O : must be more that 1 triple
        ! uri.endsWith(".html") &&
        ! uri.endsWith(".htm")
        )
    <a class="btn btn-default btn-xs" href={ s"$toolURLprefix$link" }
    title={s"Draw RDF graph diagram with $toolname for $uri"}
    target="_blank">
			<img width={imgWidth.toString()} border="0" src="https://www.w3.org/RDF/icons/rdf_flyer.svg"
           alt="RDF Resource Description Framework Flyer Icon"/>
    </a>
    else NodeSeq.Empty
  }

  val spoggyToolURL = "https://scenaristeur.github.io/spoggy-simple/?source="

  def makeDrawGraphLinkSpoggy( uri: String): NodeSeq = {
    makeDrawGraphLink( uri,
         spoggyToolURL,
//          "sparql=" + urlEncode(servicesURIPrefix) +
//          "&url=",
          "Spoggy")
  }

  def makeDrawGraphLinkLodLive( uri: String): NodeSeq = {
    makeDrawGraphLink( uri,
        "http://en.lodlive.it/?",
          "LodLive")
  }

  def makeNeighborhoodLink( uri: String,
      request: HTTPrequest,
      toolURLprefix: String = "/history?uri=",
      icon: String = "/assets/images/radial_layout.png"): NodeSeq = {
    val toolname = I18NMessages.get("Neighborhood", request.getLanguage())
    <span class="sf-navigation-button">{
    makeToolLink( uri, request, toolURLprefix, toolname, icon, imgWidth=25 )
    }</span>
  }

  /** make link to WebVOWL
   *  TODO: paste of preceding function !!!!!!!!!!!!! */
  def makeDrawGraphLinkVOWL( uri: String,
      icon: String = "http://visualdataweb.de/webvowl/favicon.ico"
      ): Elem = {
    makeToolLink( uri, HTTPrequest(), icon=icon )
  }

  /** make link to OOPS */
  def makeOOPSlink( uri: String,
      toolURLprefix: String = "http://oops.linkeddata.es/response.jsp?uri=",
      toolname: String = "OOPS (OntOlogy Pitfall Scanner)",
      icon: String = "http://oops.linkeddata.es/images/logoWhite65.png"): Elem = {
    makeToolLink( uri, HTTPrequest(), toolURLprefix, toolname, icon, imgWidth=35 )
  }

  /** make link to external tool (generic) */
  private def makeToolLink( uri: String,
      request: HTTPrequest,
      toolURLprefix: String="http://visualdataweb.de/webvowl/#iri=",
      toolname: String="Web VOWL",
      icon: String = "https://www.w3.org/RDF/icons/rdf_flyer.svg",
      imgWidth:Int=25): Elem = {
    val link = urlEncode( uri)

    if( uri  =/=  "" )
      <a class="sf-button btn-default" href={ s"$toolURLprefix$link" }
      title={s"${I18NMessages.get("Launch_tool",request.getLanguage())} $toolname for <$uri>"}
      target="_blank">
      <img width={imgWidth.toString()} style="border:1px solid"
        src={icon}
        alt={toolname}/>
      </a>
    else
      <div></div>
  }

  /** show Continuation Form: print offset, limit, pattern, Sub-form For Continuation */
  def showContinuationForm( request: HTTPrequest, formaction: Option[String]=None ) = {
//    println(s"showContinuationForm: request $request")
    val requestPath = request.path
    val requestKind = request.path . replace("/", "")
    implicit val _ = request
    <form role="form" >
      <p>{ messRequest(formaction.getOrElse(s"$requestKind")) }
         { messRequest("with") }
         { messRequest("offset") } {offsetInt(request)},
         { messRequest("limit") } {limitInt(request)},
         { messRequest("pattern") } "{paramAsString("pattern", request)}" </p>
      { makeSubformForOffsetLimit(request) }
      <input value={ messRequest("continuation") } type="submit"
             formaction={ formaction.getOrElse(s"$requestPath") } />
  </form>
  }

  /** show Sub-form For Continuation: offset, limit, pattern */
  private def makeSubformForOffsetLimit( implicit request: HTTPrequest ): NodeSeq = {
    def simpleFormField(label: String, default: String = "", increment: Int=0) = {
      val value = inputFromRequestWihIncrement( label, request, default, increment)
//    logger.debug(s"==== makeSubformForOffsetLimit: label $label, request, default $default, increment $increment ==> 'value'")
      <label for={label}>&nbsp;{ messRequest(label) }&nbsp;</label>
      <input name={label} value={value}></input>
    }

    simpleFormField("offset", "1", increment=limitInt(request)) ++
    simpleFormField("limit", defaultSPARQLlimit.toString()) ++
    simpleFormField("pattern") ++ // re-send HTTP GET parameters From Request:
    ( for((name, values) <- request.queryString) yield {
      <input type="hidden" name={name} value={values.headOption.getOrElse("")}></input>
    } )
  }

  /** */
  def inputFromRequestWihIncrement(
    label:   String,
    request: HTTPrequest, default: String = "", increment: Int = 0): String = {
    val valueOption = request.getHTTPparameterValue(label)
    toInt(valueOption, default) match {
      case Some(int) => (int + increment).toString()
      case None      => valueOption.getOrElse(default)
    }
  }

  def limitInt(request: HTTPrequest) = toInt(
      request.getHTTPparameterValue("limit").getOrElse(defaultSPARQLlimit.toString()) )
      . getOrElse(defaultSPARQLlimit)
  def offsetInt(request: HTTPrequest) = toInt(
      request.getHTTPparameterValue("offset").getOrElse("1") ) . getOrElse(1)
  def paramAsString(param: String, request: HTTPrequest) =
    request.getHTTPparameterValue(param).getOrElse("") 

  def toInt(s: Option[String], default: String = ""):Option[Int] = {
    s match {
      case Some(s) => toInt(s)
      case None =>
        toInt(default)
    }
  }
  def toInt(s: String):Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: NumberFormatException => None
    }
  }

}
