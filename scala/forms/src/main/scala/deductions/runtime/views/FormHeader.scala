package deductions.runtime.views

import java.net.URLEncoder

import scala.xml.Elem

import org.w3.banana.RDF

import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.utils.I18NMessages

trait FormHeader[Rdf <: RDF] {
self: ApplicationFacadeImpl[Rdf, _] =>
  
  /** title and links on top of the form */
  def titleEditDisplayDownloadLinks(uri: String, lang: String)
    (implicit graph: Rdf#Graph)
  : Elem = {
		def mess(m: String) =  I18NMessages.get(m, lang)  
    <div class="container">
      <div class="row">
        <h3>
          { mess("Properties_for") }
          <b>
            <a href={ hrefEditPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("edit_URI") }>
              { labelForURI(uri, lang) }
            </a>
            , URI :
            <a href={ hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("display_URI") } >{ uri }</a>
            <a href={ s"/backlinks?q=${URLEncoder.encode(uri, "utf-8")}" } title={ mess("links_towards_URI") } >o--></a>
          </b>
        </h3>
      </div>
      <div class="row">
        <div class="col-md-6">
          <a href={ uri } title="Download from original URI">{mess("Download_original_URI")}</a>
        </div>
        <div class="col-md-6">
          <a href={ hrefDownloadPrefix + URLEncoder.encode(uri, "utf-8") } title={mess("Triples_tooltip")} >
          {mess("Triples")}
          </a>
        </div>
      </div>
    </div>
  }

}
