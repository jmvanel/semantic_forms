package deductions.runtime

import org.apache.logging.log4j.{LogManager, Logger}

/** semantic logs: application logs (especially user actions) stored in SPARQL */
package object semlogs {
  private[semlogs] val logger: Logger = LogManager.getLogger("semlogs")
  println(s""">>>> package object semlogs: logger "$logger" ${logger.getClass} ${logger.getMessageFactory}""")
}