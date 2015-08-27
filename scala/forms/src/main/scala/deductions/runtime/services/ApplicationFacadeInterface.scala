package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem
import play.api.libs.iteratee.Enumerator
import scala.util.Try

/**
 * @author jmv
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 *
 *   interface with no implementation
 */
trait ApplicationFacadeInterface {

  def htmlForm(uri: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en"): Elem

  def wordsearch(q: String = ""): Future[Elem]

  def download(url: String): Enumerator[Array[Byte]]

  def saveForm(request: Map[String, Seq[String]], lang: String = ""): Elem

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem

  def sparqlSelectQuery(query: String, lang: String = "en"): Elem

  def backlinks(q: String = ""): Future[Elem]

  def esearch(q: String = ""): Future[Elem]

  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String

  def ldpGET(uri: String, accept: String): String

  def ldpPUT(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String]): Try[String]
  
  def login(loginName: String, password: String): Option[String]

  def signin(agentURI: String, password: String): Try[String]

}