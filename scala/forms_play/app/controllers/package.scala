import org.apache.logging.log4j.LogManager
import com.typesafe.scalalogging.Logger

/** */
package object controllers {
  // TODO migrate to scala-logging
  val loggerNew = Logger("server")

  val logger = LogManager.getLogger("server")
}