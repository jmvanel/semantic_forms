package controllers

import deductions.runtime.utils.DefaultConfiguration
import play.api.Play

import scala.util.Try
import scala.util.Failure
import scala.util.Success

/** Play specific additions to Default SF Configuration */
class PlayDefaultConfiguration(configuration: play.api.Configuration)
  extends DefaultConfiguration {
    /**
     * CAUTION: after activating this, be sure to to run
     * deductions.runtime.jena.lucene.TextIndexerRDF
     */
    // override val useTextQuery = true

  def this() { this(Play.current.configuration) }

  override val serverPort = makeServerPort(configuration)
  def makeServerPort(configuration: play.api.Configuration) = {
      val tr = Try {
        val port = configuration.get[String]("play.server.http.port")
        port match {
          //        case Some(port) =>
          case port if (port != null && port != "") =>
            println(s"Running on port $port")
            port
          case _ =>
            val serverPortFromConfig = super.serverPort
            logger.error(
              s"""port '$port' Could not get port from Play configuration;
              retrieving default port from SF config: '$serverPortFromConfig'""")
            serverPortFromConfig
        }
      }
      tr match {
        case Failure(e) =>
          val serverPortFromConfig = super.serverPort
          logger.error(
            s"""SF Error! Could not get port from Play configuration;
            retrieving default port from SF config: '$serverPortFromConfig'")
            Play! Message: '${e.getLocalizedMessage()}'""")
          serverPortFromConfig
        case Success(port) => port
      }
    }
}
