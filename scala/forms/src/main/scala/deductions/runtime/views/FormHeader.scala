package deductions.runtime.views

import java.net.URLEncoder

import deductions.runtime.core.FormModule
import deductions.runtime.html.BasicWidgets
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.{Configuration, I18NMessages, RDFHelpers, RDFPrefixes}
import org.w3.banana.{OWLPrefix, PointedGraph, RDF}

import scala.xml.{NodeSeq, Text}
import deductions.runtime.html.Form2HTMLDisplay
import deductions.runtime.core.HTTPrequest

/** generic application: links on top of the form: Edit, Display, Download Links
 *  TODO rename GenericApplicationHeader */
trait FormHeader[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with BasicWidgets
    with RDFPrefixes[Rdf]
    with Form2HTMLDisplay[Rdf#Node, Rdf#URI] {

//  self: ApplicationFacadeImpl[Rdf, _] =>

  val config: Configuration
  import config._
  import ops._

  /** Make URL for downloading RDF from this SF site */
  def downloadURI(uri: String, syntax: String = "Turtle"): String = {
    hrefDownloadPrefix +
      URLEncoder.encode(uri, "utf-8") +
      s"&syntax=$syntax"
  }

  /** title and links on top of the form: Edit, Display, Download Links */
  def titleEditDisplayDownloadLinksThumbnail(formSyntax: FormSyntax,
      editable: Boolean = false,
      request: HTTPrequest
  )(implicit graph: Rdf#Graph): NodeSeq = {
    val uri = nodeToString(formSyntax.subject)
    implicit val lang = request.getLanguage

    // button to change the current editable state
    val editOrUnEditButton =
      if (editable)
        hyperlinkForDisplayingURI(uri, lang)
      else
        hyperlinkForEditingURI(uri, lang)

    val expertLinksOWL = (if (showExpertButtons && isOWLURI(uri) ) {
      new Text("  ") ++
      makeDrawGraphLinkVOWL(uri) ++
      new Text("  ") ++
      makeOOPSlink(uri)
    } else NodeSeq.Empty )

    def downloadLink(syntax: String = "Turtle"): NodeSeq = {
      <span class="sf-local-rdf-link">
        <a href={ downloadURI(uri, syntax) }
        title={ mess("Triples_tooltip") }>
          { mess("Triples") + " " + syntax}
        </a>
        -
      </span>
    }

    <div class="row">
        <div class="col-xs-12">
          <h3 id="subject">
            {
              formSyntax.title
            }
            <strong>
              { editOrUnEditButton ++
                makeBackLinkButton(uri, "", request) ++
                makeNeighborhoodLink(uri, request)
              }
              { expertLinks(uri, request) }
              { expertLinksOWL }
            </strong>
            { if (formSyntax.thumbnail.isDefined) {
                val url = introduceProxyIfnecessary(formSyntax.thumbnail.get.toString(), request)
                <a class="image-popup-vertical-fit" href={ url } title={ s"Image of ${formSyntax.title}: ${formSyntax.subject.toString()}" }>
                  <img src={ url }
                    css="sf-thumbnail" height="40" alt={
                    s"Image of ${formSyntax.title}: ${formSyntax.subject.toString()}"
                  }/>
                </a>
              } else NodeSeq.Empty
            }
            { creationButton(
                 nodeToString(formSyntax.subject),
                 formSyntax.types() . map( _ . toString ),
                 request
              )
            }
          </h3>
        </div>
      </div>
      <div class="sf-links-row">
        {
          <span class="sf-local-rdf-link">Data export: </span> ++
          downloadLink() ++
          downloadLink("JSON-LD") ++
          downloadLink("RDF/XML")}
      </div>
  }

  def mess(m: String)(implicit lang: String) = I18NMessages.get(m, lang)

  private lazy val owl = OWLPrefix[Rdf]

  def isOWLURI(uri: String): Boolean = {
//    val triples = find(allNamedGraph, URI(uri), ANY, ANY).toList
//    val v = URI(uri) / rdf.typ
    logger.debug(s"isOWLURI(uri=$uri)")
    val pg = PointedGraph( URI(uri), allNamedGraph )
    val types = ( pg / rdf.typ ) . nodes . toList
    logger.debug( s"isOWLURI: $types" )
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
