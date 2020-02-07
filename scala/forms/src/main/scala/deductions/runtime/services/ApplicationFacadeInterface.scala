package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
//import play.api.libs.iteratee.Enumerator

import scala.concurrent.Future
import scala.util.Try
import scala.xml.{Elem, NodeSeq}

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
    formuri: String = "",
    graphURI: String = "",
    database: String = "TDB", request: HTTPrequest = HTTPrequest()): (NodeSeq, Boolean)

  def saveForm(request: Map[String, Seq[String]], lang: String = "",
    userid: String = "", graphURI: String = "", host: String = ""): (Option[String], Boolean)

  def create(uri: String,
    formSpecURI: String = "", graphURI: String = "", request: HTTPrequest = HTTPrequest()): NodeSeq

  def wordsearch(q: String = "", lang: String = "", clas: String = ""): Future[Elem]

  def lookup(search: String, lang: String = "en", clas: String = "", mime: String = ""): String

  /** implements download of RDF content from HTTP client */
//  def download(url: String, mime: String): Enumerator[Array[Byte]]

  def sparqlConstructQuery(query: String, lang: String = "en"): NodeSeq

  def selectSPARQL(query: String, lang: String = "en"): NodeSeq

  def backlinks(q: String = ""): Future[Elem]

  def esearch(q: String = ""): Future[Elem]

  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String

  def ldpGET(uri: String, rawURI: String, accept: String, request: HTTPrequest = HTTPrequest()): String

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest = HTTPrequest()): Try[String]

  def checkLogin(loginName: String, password: String): Boolean

  def signin(agentURI: String, password: String): Try[String]

  def claimIdentityAction(uri: String)

  /**
   * action="register"
   *  register from scratch;
   *  new account: foaf:Person creation + entering password
   */
  def registerAction(uri: String)

  def labelForURITransaction(uri: String, language: String): String
}
