package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.core.FormModule
import deductions.runtime.html.BasicWidgets
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.{Configuration, I18NMessages, RDFHelpers, RDFPrefixes}
import org.w3.banana.{OWLPrefix, PointedGraph, RDF}

import scala.xml.{NodeSeq, Text}

trait FormHeader[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with BasicWidgets
    with RDFPrefixes[Rdf] {

//  self: ApplicationFacadeImpl[Rdf, _] =>

  val config: Configuration
  import config._
  import ops._

  /** title and links on top of the form: Edit, Display, Download Links */
  def titleEditDisplayDownloadLinksThumbnail(formSyntax: FormSyntax, lang: String, editable: Boolean = false)(implicit graph: Rdf#Graph): NodeSeq = {
    def mess(m: String) = I18NMessages.get(m, lang)
    val uri = nodeToString(formSyntax.subject)

    // show the button to change the current editable state
    val linkToShow = (if (editable) {
      val hrefDisplay = hrefDisplayPrefix + URLEncoder.encode(uri, "utf-8")
      <a class="btn btn-warning btn-xs" href={ hrefDisplay } title={ mess("display_URI") }>
        <i class="glyphicon"></i>
      </a>
    } else {
      val hrefEdit = hrefEditPrefix + URLEncoder.encode(uri, "utf-8")
      <a class="btn btn-primary btn-xs" href={ hrefEdit } title={ mess("edit_URI") }>
        <i class="glyphicon glyphicon-edit"></i>
      </a>
    })

    val expertLinks = (if (showExpertButtons) {
      Seq(makeBackLinkButton(uri),
        new Text("  "),
        makeDrawGraphLink(uri))
    } else new Text(""))
    
    val expertLinksOWL = (if (showExpertButtons && isOWLURI(uri) ) {
      new Text("  ")
      makeDrawGraphLinkVOWL(uri)
    } else new Text(""))

      <div class="row">
        <div class="col-xs-12">
          <h3 id="subject">
            { formSyntax.title
            }
            <strong>
              { linkToShow }
              { expertLinks }
              { expertLinksOWL }
            </strong>
            {
            if (formSyntax.thumbnail.isDefined){
              <a class="image-popup-vertical-fit" href={  formSyntax.thumbnail.get.toString() } title={s"Image of ${formSyntax.title}: ${formSyntax.subject.toString()}"}>
                <img src={ formSyntax.thumbnail.get.toString() } css="sf-thumbnail" height="40" alt={
              s"Image of ${formSyntax.title}: ${formSyntax.subject.toString()}"
              }/></a>
            }
            else NodeSeq.Empty
            }
          </h3>
        </div>
      </div>
    <div class="row sf-links-row">
      <!--div class="col-md-6"-->
      <div class="col-xs-12 sf-local-rdf-link">
        {
          val message = if (uri.contains("/ldp/"))
            "Download local URI"
          else
            mess("Download_original_URI")
          <a href={ uri }>{ message }</a>
        }
      </div>
      <div class="col-xs-12 sf-local-rdf-link">
        <a href={ hrefDownloadPrefix + URLEncoder.encode(uri, "utf-8") } title={ mess("Triples_tooltip") }>
          { mess("Triples") }
        </a>
      </div>
    </div>
  }

  private lazy val owl = OWLPrefix[Rdf]

  def isOWLURI(uri: String): Boolean = {
    val triples = find(allNamedGraph, URI(uri), ANY, ANY).toList
//    val v = URI(uri) / rdf.typ
    val pg = PointedGraph( URI(uri), allNamedGraph )
    val types = ( pg / rdf.typ ) . nodes . toList
    println( s"isOWLURI: $types" )
    types . contains( owl.Class) ||
    types . contains( owl.ObjectProperty) ||
    types . contains( owl.DatatypeProperty) ||
    types . contains( owl.Ontology)
  }

///** NON transactional */
//  private def labelForURI(uri: String, language: String)
//  (implicit graph: Rdf#Graph)
//    : String = {
//      instanceLabel(URI(uri), graph, language)
//  }
}
