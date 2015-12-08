package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

import org.w3.banana.RDF

import play.api.libs.iteratee.Enumerator

/**
 * @author jmv
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 */
trait ApplicationFacade[Rdf <: RDF, DATASET]
		extends ApplicationFacadeInterface
    with Configuration {

  val impl: ApplicationFacadeImpl[Rdf, DATASET]
//{
//    override val recordUserActions = recordUserActions
//  }
  
  def htmlForm(uri: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en") =
    impl.htmlForm(uri: String, blankNode,
      editable, lang)

  def create(classUri: String, lang: String, formSpecURI: String)
  : NodeSeq =
    impl.create(classUri, lang, formSpecURI).get

  def lookup(search: String): String =
    impl.lookup(search)

  def wordsearch(q: String = "", lang: String = ""): Future[Elem] =
    impl.wordsearchFuture(q, lang)

  def showNamedGraphs(lang: String = ""): Future[Elem] =
    impl.showNamedGraphs(lang)
    
  def download(url: String): Enumerator[Array[Byte]] =
    impl.download(url)

  def saveForm(request: Map[String, Seq[String]], lang: String = ""): NodeSeq =
    impl.saveForm(request, lang)

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem =
    impl.sparqlConstructQuery(query, lang)

  def sparqlSelectQuery(query: String, lang: String = "en"): Elem =
    impl.selectSPARQL(query, lang)

  def backlinks(q: String = ""): Future[Elem] =
    impl.backlinksFuture(q)

  def esearch(q: String = ""): Future[Elem] =
    impl.esearchFuture(q)

  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String =
    impl.labelForURITransaction(uri, language)

  def ldpGET(uri: String, accept: String): String =
    impl.getTriples(uri, accept)

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String], content: Option[String]): scala.util.Try[String] =
    impl.ldpPOST(uri, link, contentType, slug, content)

  def checkLogin(loginName: String, password: String): Boolean =
    impl.checkLogin(loginName, password)

  def signin(agentURI: String, password: String): scala.util.Try[String] =
    impl.signin(agentURI, password)

  def findUser(loginName: String): Option[String] =
    impl.findUser(loginName)

  def displayUser(userid: String, pageURI: String, pageLabel: String, lang: String = "en") =
    impl.displayUser(userid, pageURI, pageLabel, lang)

  def claimIdentityAction(uri: String) =
    impl.claimIdentityAction(uri)

  /**
   * action="register"
   *  register from scratch;
   *  new account: foaf:Person creation + entering password
   */
  def registerAction(uri: String)
//    (implicit graph: Rdf#Graph)
    = impl.registerAction(uri)
}
