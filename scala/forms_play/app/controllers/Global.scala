import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem
import deductions.runtime.sparql_cache.PopulateRDFCache
import org.apache.log4j.Logger

package global {

object Global extends play.api.GlobalSettings {
  var form : Elem = <p>initial value</p>
  val tv = new TableView {}
  
  override def onStart(app: Application) {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    PopulateRDFCache.loadCommonVocabularies
    form = htmlForm(uri)
  }

    def htmlForm(uri: String, blankNode:String=""): scala.xml.Elem = {
      Logger.getRootLogger().info("Global.htmlForm uri "+ uri)
      if (uri != null && uri != "")
        tv.htmlForm(uri, "/display?displayuri=", blankNode )
      else
        <p>Enter an URI</p>
    }
}
}