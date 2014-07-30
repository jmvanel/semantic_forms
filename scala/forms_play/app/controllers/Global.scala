import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem
import deductions.runtime.sparql_cache.PopulateRDFCache

package global {

object Global extends play.api.GlobalSettings {
  var form : Elem = <p>initial value</p>
  val tv = new TableView {}
  
  override def onStart(app: Application) {
    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
    PopulateRDFCache.loadCommonVocabularies
    
//    global.Global.
    form = htmlForm(uri)
  }

    def htmlForm(uri: String): scala.xml.Elem = {
      if (uri != null && uri != "")
        tv.htmlForm(uri)
      else
        <p>Enter an URI</p>
    }
}

//  object Global {
//      var form : Elem = <p>initial value</p>
//  }
}