package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

import org.w3.banana.RDF

import play.api.libs.iteratee.Enumerator
import deductions.runtime.utils.HTTPrequest

/**
 * @author jmv
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 */
trait ApplicationFacade[Rdf <: RDF, DATASET]
		extends ApplicationFacadeInterface
    with Configuration
    {

  val impl: ApplicationFacadeImpl[Rdf, DATASET]
  
  def htmlForm(uri: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en", formuri: String = "",
    graphURI: String = "" ) =
    impl.htmlForm(uri: String, blankNode,
      editable, lang, formuri, graphURI)

  def htmlFormElemJustFields(uri: String, hrefPrefix: String = "", blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en", formuri: String
    , graphURI: String = ""
    )
    : NodeSeq = {
    impl.htmlFormElemJustFields(uri, hrefPrefix, blankNode, editable, lang, formuri=formuri
            , graphURI=graphURI )
  }
  
  def create(classUri: String, lang: String, formSpecURI: String, graphURI: String, request: HTTPrequest )
  : NodeSeq =
    impl.create(classUri, lang, formSpecURI, graphURI, request: HTTPrequest).get

  def lookup(search: String): String =
    impl.lookup(search)

  def wordsearch(q: String = "", lang: String = ""): Future[Elem] =
    impl.wordsearchFuture(q, lang)

  def showNamedGraphs(lang: String = ""): Future[NodeSeq] =
    impl.showNamedGraphs(lang)
    
  def showTriplesInGraph(graphURI: String, lang: String = "")
//  : Future[Elem] 
  =
    impl.showTriplesInGraph(graphURI: String, lang)
    
  def download(url: String, mime: String="text/turtle"): Enumerator[Array[Byte]] =
    impl.download(url, mime)

  /** @see [[ApplicationFacadeImpl.saveForm]] */
  def saveForm(request: Map[String, Seq[String]], lang: String = "", userid: String="",
      graphURI: String = "", host: String= ""): Option[String] =
    impl.saveForm(request, lang, userid, graphURI, host)

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem =
    impl.sparqlConstructQuery(query, lang)

  // TODO lang useful ????
  def sparqlConstructResult(query: String, lang: String = "en", format: String="turtle"): String = {
	  impl.sparqlConstructResult(query, lang, format)
  }

  def sparqlSelectQuery(query: String, lang: String = "en"): Elem =
    impl.selectSPARQL(query, lang)

  def backlinks(q: String = ""): Future[Elem] =
    impl.backlinksFuture(q)

  def esearch(q: String = ""): Future[Elem] =
    impl.esearchFuture(q)
    
  /** NOTE this creates a transaction; do not use it too often */
  def labelForURI(uri: String, language: String): String =
    impl.labelForURITransaction(uri, language)

  def ldpGET(uri: String, rawURI: String, accept: String, request: HTTPrequest): String =
    impl.getTriples(uri, rawURI, accept, request)

  def ldpPOST(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String], content: Option[String], request: HTTPrequest): scala.util.Try[String] =
    impl.ldpPOST(uri, link, contentType, slug, content, request)

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
    
  def makeHistoryUserActions(userURI: String, lang: String, request: HTTPrequest): NodeSeq =
    impl.makeHistoryUserActions(userURI, lang, request)

  def labelForURITransaction(uri: String, language: String) =
   impl.labelForURITransaction(uri: String, language: String)

}
