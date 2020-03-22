package deductions.runtime

import org.apache.logging.log4j.LogManager

/** package for Jena specific stuff */
package object jena {
  implicit val logger = LogManager.getLogger("jena")
}