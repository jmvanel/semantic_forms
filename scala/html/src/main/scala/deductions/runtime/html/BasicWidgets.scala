package deductions.runtime.html

import java.net.URLEncoder

import deductions.runtime.utils.RDFPrefixesInterface

import scala.xml.Elem
import deductions.runtime.utils.URIHelpers
import scala.xml.NodeSeq
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.I18NMessages

import scalaz._
import Scalaz._

/** Basic Widgets with no access to the FormSyntax:
 *  GUI integration: RDFviewer, VOWL, ... */
trait BasicWidgets
  extends RDFPrefixesInterface
  with URIHelpers {

    val config: Configuration
    import config._

  def hyperlinkForEditingURI(uri: String, lang: String): NodeSeq = {
    implicit val _ = lang
    val hrefEdit = hrefEditPrefix + URLEncoder.encode(uri, "utf-8")
    <a class="btn btn-primary btn-xs" href={ hrefEdit } title={ mess("edit_URI") }>
      <i class="glyphicon glyphicon-edit"></i>
    </a>
  }

  def hyperlinkForEditingURIinsideForm(uri: String, lang: String): NodeSeq = {
    if( ! uri.startsWith("http://dbpedia.org/resource/"))
      hyperlinkForEditingURI(uri, lang)
    else NodeSeq.Empty
  }

  def hyperlinkForDisplayingURI(uri: String, lang: String): NodeSeq = {
    implicit val _ = lang
    val hrefDisplay = hrefDisplayPrefix() + URLEncoder.encode(uri, "utf-8") + "#subject"
    println(s">>>>>>>>>>> linkToShow: $hrefDisplay")
    <a class="btn btn-warning btn-xs" href={ hrefDisplay } title={ mess("display_URI") }>
      <i class="glyphicon"></i>
    </a>
  }

  private def mess(m: String)(implicit lang: String) = I18NMessages.get(m, lang)

  def makeBackLinkButton(uri: String, title: String = ""): Elem = {
    val tit = if (title === "") s" Reverse links for &lt;$uri&gt;" else title
    return <a class="btn btn-default btn-xs"
      href={
      // URLEncoder.encode(uri,"UTF-8")
      "/backlinks?q=" + uri
      }
      title={ tit }
      data-value={s"$uri"} >
      <img src="assets/images/Back-Link-Icon.svg" width="15" border="0" />
    </a>
  }

  def makeDrawGraphLink( uri: String,
      toolURLprefix: String="/assets/rdfviewer/rdfviewer.html?url=",
      toolname: String="RDF Viewer",
      imgWidth:Int=15): NodeSeq = {

    // TODO different link when we are on localhost (transmit RDF String then) or in production (or use N3.js
    // http://localhost:9000/download?url=http%3A%2F%2Fjmvanel.free.fr%2Fjmv.rdf%23me
    val link = /*hrefDownloadPrefix + */ URLEncoder.encode( uri, "utf-8")

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

  def makeNeighborhoodLink( uri: String,
      toolURLprefix: String = "/history?uri=",
      toolname: String =
        "Neighborhood: \npaths of length <= 2 sorted in chronological order\n",
      icon: String = "/assets/images/radial_layout.png"): Elem = {
    makeToolLink( uri, toolURLprefix, toolname, icon, imgWidth=35 )
  }

  /** make link to WebVOWL
   *  TODO: paste of preceding function !!!!!!!!!!!!! */
  def makeDrawGraphLinkVOWL( uri: String,
      icon: String = "http://visualdataweb.de/webvowl/favicon.ico"
      ): Elem = {
    makeToolLink( uri, icon=icon )
  }

  /** make link to OOPS */
  def makeOOPSlink( uri: String,
      toolURLprefix: String = "http://oops.linkeddata.es/response.jsp?uri=",
      toolname: String = "OOPS (OntOlogy Pitfall Scanner)",
      icon: String = "http://oops.linkeddata.es/images/logoWhite65.png"): Elem = {
    makeToolLink( uri, toolURLprefix, toolname, icon, imgWidth=35 )
  }

  /** make link to external tool (generic) */
  private def makeToolLink( uri: String,
      toolURLprefix: String="http://visualdataweb.de/webvowl/#iri=",
      toolname: String="Web VOWL",
      icon: String = "https://www.w3.org/RDF/icons/rdf_flyer.svg",
      imgWidth:Int=25): Elem = {
    val link = URLEncoder.encode( uri, "utf-8")

    if( uri != "" )
      <a class="btn btn-default" href={ s"$toolURLprefix$link" }
      title={s"Launch tool $toolname for <$uri>"}
      target="_blank">
      <img width={imgWidth.toString()} border="0"
        src={icon}
        alt={toolname}/>
      </a>
    else
      <div></div>
  }

}