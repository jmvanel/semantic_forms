import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem
import deductions.runtime.sparql_cache.PopulateRDFCache
import org.apache.log4j.Logger
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.StringSearchSPARQL
import java.net.URLEncoder

package global {

object Global extends play.api.GlobalSettings {
  var form : Elem = <p>initial value</p>
  lazy val tv = new TableView {}
  lazy val store =  RDFStoreObject.store
  lazy val search = new StringSearchSPARQL(store)
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="

  override def onStart(app: Application) {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    PopulateRDFCache.loadCommonVocabularies
    form = htmlForm(uri)
  }

    def htmlForm(uri0: String, blankNode:String=""): Elem = {
      Logger.getRootLogger().info("Global.htmlForm uri "+ uri +
          " blankNode \"" + blankNode + "\"" )
      val uri = uri0.trim()
      URLEncoder.encode(s, enc)

      <p>Properties for URI {uri}
      <a href="{uri}" title="Download HTML">HTML</a>
      <a href={hrefDownloadPrefix + URLEncoder.encode(uri,"utf-8")} title="Download Turtle">Triples</a>
      <br/>
      {if (uri != null && uri != "")
        try {
        tv.htmlForm(uri, hrefDisplayPrefix, blankNode )
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
        "work in progress!" // TODO BrowsableGraph focusOnURI( url )
      }

}
}