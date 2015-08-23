package deductions.runtime.jena

import scala.concurrent.Future
import scala.xml.Elem

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import com.hp.hpl.jena.query.Dataset

import deductions.runtime.services.ApplicationFacadeImpl
import play.api.libs.iteratee.Enumerator

/**
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 */
trait ApplicationFacadeJena {
  /*  NOTE: important that JenaModule is first; otherwise ops may be null */
  trait ApplicationFacadeJenaDependencies extends JenaModule
    with ApplicationFacadeImpl[Jena, Dataset]
    with RDFStoreLocalJena1Provider

  val facade = new ApplicationFacadeJenaDependencies {}
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
}
