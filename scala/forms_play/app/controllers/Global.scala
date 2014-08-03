import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem
import deductions.runtime.sparql_cache.PopulateRDFCache
import org.apache.log4j.Logger
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.StringSearchSPARQL

package global {

object Global extends play.api.GlobalSettings {
  var form : Elem = <p>initial value</p>
  lazy val tv = new TableView {}
//  lazy val store =  RDFStoreObject.store
//  lazy val search = new StringSearchSPARQL(store)
  
  override def onStart(app: Application) {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    PopulateRDFCache.loadCommonVocabularies
    form = htmlForm(uri)
  }

    def htmlForm(uri: String, blankNode:String=""): scala.xml.Elem = {
      Logger.getRootLogger().info("Global.htmlForm uri "+ uri +
          " blankNode \"" + blankNode + "\"" )
      if (uri != null && uri != "")
        tv.htmlForm(uri, "/display?displayuri=", blankNode )
      else
        <p>Enter an URI</p>
    }
    

    def wordsearch(q:String="") : Elem = {
//    	search.search(q)
      <p/>
    }
}
}