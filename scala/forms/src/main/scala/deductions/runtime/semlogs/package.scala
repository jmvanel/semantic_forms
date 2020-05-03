package deductions.runtime

import com.typesafe.scalalogging.Logger

/** semantic logs: application logs (especially user actions) stored in SPARQL */
package object semlogs {
  private[semlogs] val logger = Logger("semlogs")
}