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
import scala.reflect.ClassTag

/** generic application: links on top of the form: Edit, Display, Download Links
 *  TODO rename GenericApplicationHeader */
trait FormHeader[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with BasicWidgets[Rdf#Node, Rdf#URI]
    with RDFPrefixes[Rdf]
    with Form2HTMLDisplay[Rdf#Node, Rdf#URI] {

//  self: ApplicationFacadeImpl[Rdf, _] =>

  val config: Configuration
  import config._
  import ops._

  /** Make URL for downloading RDF from this SF site */
  def downloadURI(uri: String,
      request: HTTPrequest,
      syntax: String = "Turtle"): String = {              
    val isBlanknode = request.getHTTPparameterValue("blanknode").getOrElse("") == "true"
    val uri1 = (if(isBlanknode) "_:" else "" ) + uri
    hrefDownloadPrefix +
      URLEncoder.encode(uri1, "utf-8") +
      s"&syntax=$syntax"
  }

  /** title and links on top of the form: Edit, Display, Download Links
   *  @See also #Form2HTML.dataFormHeader() */
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

    def downloadLink(request: HTTPrequest, syntax: String = "Turtle"): NodeSeq = {
      <span class="sf-local-rdf-link">
        <a href={ downloadURI(uri, request, syntax) }
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
              linkToFormSubject(formSyntax, request.getLanguage)
              // formSyntax.title
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
            { 

              // formSyntax.typeEntries() is not very robust, it can return literal "types"
              val typeEntry = asInstanceOfOption[FormModule[Rdf#Node,Rdf#URI]#ResourceEntry](
                  formSyntax.typeEntries().headOption.getOrElse(NullResourceEntry))
              creationButton(
                 Seq("#Class"),
                 clone=true,
                 request, typeEntry.getOrElse(NullResourceEntry)
              )
            }
          </h3>
        </div>
      </div>
      <div class="sf-links-row">
        <span class="sf-local-rdf-link">Data export: </span>
        {
          downloadLink(request) ++
          downloadLink(request, "JSON-LD") ++
          downloadLink(request, "RDF/XML") ++
          downloadLink(request, "N-TRIPLES")}
      </div>
  }

  /** cf https://stackoverflow.com/questions/1803036/how-to-write-asinstanceofoption-in-scala */
  private def asInstanceOfOption[T: ClassTag](o: Any): Option[T] =
    Some(o) collect { case m: T => m}

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
