package controllers

import deductions.runtime.utils.DefaultConfiguration
import play.api.Play

import scala.util.Try
import scala.util.Failure
import scala.util.Success

/** Play specific additions to Default SF Configuration */
class PlayDefaultConfiguration extends DefaultConfiguration {
    /**
     * CAUTION: after activating this, be sure to to run
     * deductions.runtime.jena.lucene.TextIndexerRDF
     */
    // override val useTextQuery = true

    override val serverPort = {
      val tr = Try {
        val port = Play.current.configuration.
          get[String]("http.port")
        port match {
          //        case Some(port) =>
          case port if (port != null && port != "") =>
            println(s"Running on port $port")
            port
          case _ =>
            val serverPortFromConfig = super.serverPort
            logger.error(
              s"Could not get port from Play configuration; retrieving default port from SF config: $serverPortFromConfig")
            serverPortFromConfig
        }
      }
      tr match {
        case Failure(e) =>
          val serverPortFromConfig = super.serverPort
          logger.error(
            s"""Could not get port from Play configuration;
            retrieving default port from SF config: $serverPortFromConfig")
              $e""")
          serverPortFromConfig
        case Success(port) => port
      }
    }
}
