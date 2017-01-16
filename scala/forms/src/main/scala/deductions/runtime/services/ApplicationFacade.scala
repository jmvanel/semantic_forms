package deductions.runtime.services

import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

import org.w3.banana.RDF

import play.api.libs.iteratee.Enumerator
import deductions.runtime.utils.HTTPrequest
import scala.util.Try

/**
 * @author jmv
 * API for Web Application, so that:
 * - client has no dependence on Banana
 * - 95% of the application is already done here, and there is no dependence
 *   to a particular Web framework
 */
trait ApplicationFacade[Rdf <: RDF, DATASET]
		extends ApplicationFacadeInterface {

//  def getRequest: HTTPrequest

  val impl: ApplicationFacadeImpl[Rdf, DATASET]
  
  def htmlForm(uri: String, blankNode: String = "",
    editable: Boolean = false,
    lang: String = "en", formuri: String = "",
    graphURI: String = "",
    database: String = "TDB", request:HTTPrequest = HTTPrequest() ) =
    impl.htmlForm(uri, blankNode,
      editable, lang, formuri, graphURI, database, request )

  def formDataImpl(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "", database: String = "TDB"): String =
    impl.formData(uri, blankNode, Edit, formuri, database)

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

  def createDataAsJSON(classUri: String, lang: String, formSpecURI: String, graphURI: String, request: HTTPrequest ): String =
    impl.createDataAsJSON(classUri, lang, formSpecURI, graphURI, request: HTTPrequest)

  def lookup(search: String, lang: String = "en", clas: String ="", mime: String=""): String =
    impl.lookup(search, lang, clas, mime)

  def wordsearch(q: String = "", lang: String = "", clas: String = ""): Future[Elem] =
    impl.wordsearchFuture(q, lang, clas)

  def showNamedGraphs(lang: String = ""): Future[NodeSeq] =
    impl.showNamedGraphs(lang)
    
  def showTriplesInGraph(graphURI: String, lang: String = "")
//  : Future[Elem] 
  =
    impl.showTriplesInGraph(graphURI: String, lang)
    
  /** implements download of RDF content from HTTP client */
  def download(url: String, mime: String="text/turtle"): Enumerator[Array[Byte]] =
    impl.download(url, mime)

  /** @see [[ApplicationFacadeImpl.saveForm]] */
  def saveForm(request: Map[String, Seq[String]], lang: String = "", userid: String="",
      graphURI: String = "", host: String= ""): Option[String] =
    impl.saveForm(request, lang, userid, graphURI, host)

  def sparqlConstructQuery(query: String, lang: String = "en"): Elem =
    impl.sparqlConstructQuery(query, lang)

  // TODO lang useful ????
  def sparqlConstructResult(query: String, format: String="turtle", lang: String = "en"): String = {
	  impl.sparqlConstructResult(query, lang, format)
  }

  def sparqlSelectConneg(queryString: String, format: String="turtle", ds: DATASET ) =
    impl.sparqlSelectConneg(queryString, format, ds)

  def sparqlSelectQuery(query: String, lang: String = "en"): Elem =
    impl.selectSPARQL(query, lang)

  /** create JSON Form From SPARQL */
  def createJSONFormFromSPARQL(query: String,
                               editable: Boolean = false,
                               formuri: String = "") // : FormSyntax
                               =
    impl.createJSONFormFromSPARQL(query, editable, formuri)

  def createHTMLFormFromSPARQL(query: String,
                               editable: Boolean = false,
                               formuri: String = "") =
    impl.createHTMLFormFromSPARQL(query, editable, formuri) 
    
  def sparqlUpdateQuery(queryString: String): Try[Unit] =
    impl.sparqlUpdateQueryTR(queryString)


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
