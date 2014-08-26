import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem
import deductions.runtime.sparql_cache.PopulateRDFCache
import org.apache.log4j.Logger
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.StringSearchSPARQL
import java.net.URLEncoder
import deductions.runtime.services.BrowsableGraph
import play.api.mvc.Request
import deductions.runtime.services.FormSaver
import play.api.data.Form
import play.core.parsers.FormUrlEncodedParser
import play.api.mvc.Controller
import play.api.mvc.AnyContentAsFormUrlEncoded
import java.net.URLDecoder

package global {

object Global extends Controller // play.api.GlobalSettings
{
  var form : Elem = <p>initial value</p>
  lazy val tv = new TableView {}
  lazy val store =  RDFStoreObject.store
  lazy val search = new StringSearchSPARQL(store)
  lazy val dl = new BrowsableGraph(store)
  lazy val fs = new FormSaver(store)
  
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="

//  override def onStart(app: Application) {
//    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
//    PopulateRDFCache.loadCommonVocabularies
//    form = htmlForm(uri)
//  }

  def htmlForm(uri0: String, blankNode:String="",
    editable:Boolean=false ) : Elem = {
      Logger.getRootLogger().info("Global.htmlForm uri "+ uri0 +
          " blankNode \"" + blankNode + "\"" )
    val uri = uri0.trim()

    <p>
      Properties for URI {uri}
      <a href={uri} title="Download HTML from original URI">HTML</a>
      <a href={hrefDownloadPrefix + URLEncoder.encode(uri,"utf-8")}
         title="Download Turtle">Triples</a>
      <br/>
      {if (uri != null && uri != "")
        try {
          tv.htmlForm(uri, hrefDisplayPrefix, blankNode, editable )
        } catch {
        case e:Exception // e.g. org.apache.jena.riot.RiotException
        =>
          <p style="color:red">{e.getLocalizedMessage()}<br/> Cause: 
          {e.getCause().getLocalizedMessage()}
          </p>
        }
      else
        <p>Enter an URI</p>}
    </p>
  }

  def wordsearch(q:String="") : Elem = {
      <p>Searched for "{q}" :<br/>
    	  {search.search(q, hrefDisplayPrefix)}
      </p>
    }
    
  def download( url:String ) : String = {
        dl.focusOnURI( url )
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
//          println("save: " + body.toString)
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
 }
}