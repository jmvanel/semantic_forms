package controllers

import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.utils.DefaultConfiguration
import play.api.Play

/** object Application in another file to facilitate redefinition */
object Application extends {
  override implicit val config = new DefaultConfiguration {
    override val serverPort = {
      val port = Play.current.configuration.
        getString("http.port")
      port match {
        case Some(port) =>
          println( s"Running on port $port")
          port
        case _ =>
          val serverPortFromConfig = super.serverPort
          println(s"Could not get port from Play configuration; retrieving default port from SF config: $serverPortFromConfig")
          serverPortFromConfig
      }
    }
  }
} with Services
  with WebPages
  with SparqlServices {
  override lazy val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
}
