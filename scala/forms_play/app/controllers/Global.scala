import play.api.GlobalSettings
import play.api.Application
import deductions.runtime.html.TableView
import scala.xml.Elem

object Global extends GlobalSettings {
  val tv = new TableView {}
  override def onStart(app: Application) {
    val uri = "http://jmvanel.free.fr/jmv.n3"
    global.Global.form = tv.htmlForm(uri)
  }
}

package global {
  object Global {
      var form : Elem = <p>initial value</p>
  }
}