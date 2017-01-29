package deductions.runtime

import org.apache.log4j.Logger

/** semantic logs: application logs (especially user actions) stored in SPARQL */
package object semlogs {
  private[semlogs] val logger: Logger = Logger.getLogger("semlogs")
}