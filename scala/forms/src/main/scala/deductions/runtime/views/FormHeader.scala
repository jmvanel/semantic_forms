package deductions.runtime.views

import java.net.URLEncoder
import scala.xml.Elem
import org.w3.banana.RDF
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.utils.I18NMessages
import deductions.runtime.services.Configuration
import deductions.runtime.services.ApplicationFacadeInterface
import scala.xml.NodeSeq
import deductions.runtime.html.BasicWidgets
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.Unparsed

trait FormHeader[Rdf <: RDF]
		extends Configuration
    with BasicWidgets
{
self: ApplicationFacadeImpl[Rdf, _] =>
  
  /** title and links on top of the form: Edit, Display, Download Links */
  def titleEditDisplayDownloadLinks(uri: String, lang: String, editable: Boolean = false)
    (implicit graph: Rdf#Graph)
  : Elem = {
		def mess(m: String) =  I18NMessages.get(m, lang)

		val linkToShow = (if (editable) { 
      <a class="btn btn-warning" href={ hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("display_URI") } >
				<i class="glyphicon glyphicon-remove"></i> 
			</a>
		} else {
		  <a class="btn btn-primary" href={ hrefEditPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("edit_URI") }>
				<i class="glyphicon glyphicon-edit"></i> 
      </a>
		})

		val expertLinks = (if (showExpertButtons) {
		  Seq (makeBackLinkButton(uri),
           new Text("  "), 
           makeDrawGraphLink(uri)
      )
		} else new Text(""))

    <div class="container">
      <div class="row">
        <h3>
          { labelForURI(uri, lang) }
          <strong>
						{ linkToShow }
						{ expertLinks }
          </strong>
        </h3>
      </div>
      <div class="row">
        <div class="col-md-6">
          <a href={ uri } title="Download from original URI">{mess("Download_original_URI")}</a>
						&lt; { uri } &gt;
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
