package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem

import org.w3.banana.RDF

import play.api.libs.iteratee.Enumerator

/**
 * @author jmv
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 */
trait ApplicationFacade[Rdf <: RDF, DATASET] extends ApplicationFacadeInterface {

  val facade: ApplicationFacadeImpl[Rdf, DATASET]

  private implicit val allNamedGraphs2 = facade.allNamedGraphs

  def htmlForm(uri: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en") =
    facade.htmlForm(uri: String, blankNode,
      editable, lang)

  def wordsearch(q: String = ""): Future[Elem] =
    facade.wordsearchFuture(q)

  def download(url: String): Enumerator[Array[Byte]] =
    facade.download(url)

  def saveForm(request: Map[String, Seq[String]], lang: String = ""): Elem =
    facade.saveForm(request, lang)

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem =
    facade.sparqlConstructQuery(query, lang)

  def sparqlSelectQuery(query: String, lang: String = "en"): Elem =
    facade.selectSPARQL(query, lang)

  def backlinks(q: String = ""): Future[Elem] =
    facade.backlinksFuture(q)

  def esearch(q: String = ""): Future[Elem] =
    facade.esearchFuture(q)

  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String =
    facade.labelForURI(uri, language)

  def ldpGET(uri: String, accept: String): String =
    facade.getTriples(uri, accept)

  def ldpPUT(uri: String, link: Option[String], contentType: Option[String], slug: Option[String],
    content: Option[String]): scala.util.Try[String] =
    facade.ldpPUT(uri, link, contentType, slug, content)

  def login(loginName: String, password: String): Option[String] =
    facade.login(loginName, password)

  def signin(agentURI: String, password: String): scala.util.Try[String] =
    signin(agentURI, password)
}
