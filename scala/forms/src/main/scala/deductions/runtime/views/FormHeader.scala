package deductions.runtime.views

import scala.xml.Elem
import deductions.runtime.utils.I18NMessages
import java.net.URLEncoder
import org.w3.banana.RDF
import deductions.runtime.services.ApplicationFacadeImpl
import scala.xml.NodeSeq

trait FormHeader[Rdf <: RDF] {
self: ApplicationFacadeImpl[Rdf, _] =>
  
  /** title and links on top of the form */
  def titleEditDisplayDownloadLinks(uri: String, lang: String)
    (implicit graph: Rdf#Graph)
  : Elem =
    <div class="container">
      <div class="row">
        <h3>
          { I18NMessages.get("Properties_for", lang) }
          <b>
            <a href={ hrefEditPrefix + URLEncoder.encode(uri, "utf-8") } title="edit this URI">
              { labelForURI(uri, lang) }
            </a>
            , URI :
            <a href={ hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8") } title="display this URI">{ uri }</a>
            <a href={ s"/backlinks?q=${URLEncoder.encode(uri, "utf-8")}" } title="links towards this URI">o--></a>
          </b>
        </h3>
      </div>
      <div class="row">
        <div class="col-md-6">
          <a href={ uri } title="Download from original URI">Download from original URI</a>
        </div>
        <div class="col-md-6">
          <a href={ hrefDownloadPrefix + URLEncoder.encode(uri, "utf-8") } title="Download Turtle from database (augmented by users' edits)">Triples</a>
        </div>
      </div>
    </div>

}