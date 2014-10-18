import java.net.URLDecoder
import java.net.URLEncoder
import scala.xml.Elem
import org.apache.log4j.Logger
import deductions.runtime.html.TableView
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.FormSaver
import deductions.runtime.services.StringSearchSPARQL
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Controller
import play.api.mvc.Request
import deductions.runtime.html.CreationForm

package global {

object Global extends Controller // play.api.GlobalSettings
{
  var form : Elem = <p>initial value</p>
  lazy val tableView = new TableView {}
  lazy val store =  RDFStoreObject.store
  lazy val search = new StringSearchSPARQL(store)
  lazy val dl = new BrowsableGraph(store)
  lazy val fs = new FormSaver(store)
  lazy val cf = new CreationForm { actionURI = "/save" }
  
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="

//  override def onStart(app: Application) {
//    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
//    PopulateRDFCache.loadCommonVocabularies
//    form = htmlForm(uri)
//  }

  def htmlForm(uri0: String, blankNode:String="",
    editable:Boolean=false,
    lang:String="en" ) : Elem = {
      Logger.getRootLogger().info( s"""Global.htmlForm uri $uri0 blankNode "$blankNode" lang=$lang """ )
    val uri = uri0.trim()

    <p>
      Properties for URI <b>{uri}</b>
      <a href={uri} title="Download from original URI">HTML</a>
      <a href={hrefDownloadPrefix + URLEncoder.encode(uri,"utf-8")}
         title="Download Turtle from database (augmented by users' edits)">Triples</a>
      <br/>

      {if (uri != null && uri != "")
        try {
          tableView.htmlForm(uri, hrefDisplayPrefix, blankNode, editable=editable, lang=lang )
        } catch {
        case e:Exception // e.g. org.apache.jena.riot.RiotException
        =>
          <p style="color:red">{
            e.getLocalizedMessage() + " " + printTrace(e)
            } <br/>
          Cause: 
          { if ( e.getCause() != null ) e.getCause().getLocalizedMessage()}
          </p>
        }
      else
        <p>Enter an URI</p>}
    </p>
  }

  def printTrace(e: Exception) : String = {
    var s = ""
    for ( i <- e.getStackTrace() ) { s = s + " " + i }
    s
  }
  
  def wordsearch(q:String="") : Elem = {
      <p>Searched for "{q}" :<br/>
    	  {search.search(q, hrefDisplayPrefix)}
      </p>
    }
    
  def download( url:String ) : String = {
    println( "download url " + url )
    val res = dl.focusOnURI( url )
    println( "download result " + res )
    res
  }

  def edit( url:String ) : Elem = {
        htmlForm(url, editable=true)
  }

  def save(request: Request[_]): Elem = {
      val body = request.body
      body match {
        case form: AnyContentAsFormUrlEncoded =>
          val map = form.data
          println("Global.save: " + body.getClass + ", map " + map)
          try{
        	  fs.saveTriples(map)
          } catch {
          case t:Throwable => println( "Exception in saveTriples: " + t )
          // TODO show Exception to user
          }
          val uriOption = map.getOrElse("uri", Seq()).headOption
          println("Global.save: uriOption " + uriOption )
          uriOption match {
            case Some(url1) => htmlForm(
                URLDecoder.decode(url1, "utf-8"),
                editable = false )
            case _ => <p>Save: not normal: { uriOption }</p>
          }      
        case _ => <p>Save: not normal: { form.getClass() }</p>
      }
  }
  
  def create( uri0:String, lang:String="en"  ) : Elem = {
    Logger.getRootLogger().info("Global.htmlForm uri "+ uri0 )
    val uri = uri0.trim()
    
    <p>Creating an instance of Class <bold>{uri}</bold>
      {cf.create(uri, lang)}
    </p>
  }

  def sparql( query:String, lang:String="en"  ) : Elem = {
    Logger.getRootLogger().info("Global.sparql query  "+ query )
    <p>SPARQL query:<br/>{query}
    <br/>
    {dl.sparqlConstructQuery(query)}
    </p>
  }
 }
}