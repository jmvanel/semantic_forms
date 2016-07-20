package controllers
import play.api.mvc.Action
import play.api.mvc.Controller
import java.io.File

object Files  extends Controller {
  /** TODO think about security: should not be possible to download files not wanted */
  def at(file: String) = Action {
  Ok.sendFile(
    content = new java.io.File( "files" + File.separator + file),
    inline = true
  )
}
}