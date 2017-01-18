package deductions.runtime.views

import java.net.URLEncoder

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text

import org.w3.banana.RDF

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.html.BasicWidgets
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.Configuration
import deductions.runtime.utils.I18NMessages

trait FormHeader[Rdf <: RDF]
    extends FormModule[Rdf#Node, Rdf#URI]
    with BasicWidgets {

  self: ApplicationFacadeImpl[Rdf, _] =>

  val config: Configuration
  import config._

  /** title and links on top of the form: Edit, Display, Download Links */
  def titleEditDisplayDownloadLinksThumbnail(formSyntax: FormSyntax, lang: String, editable: Boolean = false)(implicit graph: Rdf#Graph): NodeSeq = {
    def mess(m: String) = I18NMessages.get(m, lang)
    val uri = nodeToString(formSyntax.subject)

    // show the button to change the current editable state
    val linkToShow = (if (editable) {
      val hrefDisplay = hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8")
      <a class="btn btn-warning" href={ hrefDisplay } title={ mess("display_URI") }>
        <i class="glyphicon"></i>
      </a>
    } else {
      val hrefEdit = hrefEditPrefix + URLEncoder.encode(uri, "utf-8")
      <a class="btn btn-primary" href={ hrefEdit } title={ mess("edit_URI") }>
        <i class="glyphicon glyphicon-edit"></i>
      </a>
    })

    val expertLinks = (if (showExpertButtons) {
      Seq(makeBackLinkButton(uri),
        new Text("  "),
        makeDrawGraphLink(uri))
    } else new Text(""))

    <div class="container">
      <div class="row">
        <h3>
          { labelForURI(uri, lang) }
          <strong>
            { linkToShow }
            { expertLinks }
          </strong>
          {
            if (formSyntax.thumbnail.isDefined)
              <img src={ formSyntax.thumbnail.get.toString() } css="sf-thumbnail" height="40" alt={
                s"Image of ${formSyntax.title}: ${formSyntax.subject.toString()}"
              }/>
              else NodeSeq.Empty
          }
        </h3>
      </div>
    </div>
    <div class="sf-links-row">
      <!--div class="col-md-6"-->
      <div class="sf-local-rdf-link">
        {
          val message = if (uri.contains("/ldp/"))
            "Download local URI"
          else
            mess("Download_original_URI")
          <a href={ uri }>{ message }</a>
        }
      </div>
      <div class="sf-local-rdf-link">
        <a href={ hrefDownloadPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("Triples_tooltip") }>
          { mess("Triples") }
        </a>
      </div>
    </div>
  }

}
