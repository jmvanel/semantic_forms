package controllers
import play.api.mvc.Action
import play.api.mvc.Controller
import java.io.File
import scala.concurrent.ExecutionContext
import javax.inject.Inject
import play.api.http.FileMimeTypes
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents


class Files @Inject()(implicit override val fileMimeTypes: FileMimeTypes,
    implicit val controllerComponents: ControllerComponents,
    implicit val ec: ExecutionContext) extends BaseController {

  /** TODO think about security: should not be possible to download files not wanted */
  def at(file: String)
  = Action {
    Ok.sendFile(
      content = new java.io.File( "files" + File.separator + file),
      inline = true
  ) }
}

