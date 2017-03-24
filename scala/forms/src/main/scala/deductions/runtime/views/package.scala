package deductions.runtime

import org.apache.logging.log4j.LogManager

/** HTML templates not for the body of forms: page header, etc; other templates for the body of forms HTML Scala templates are in package [[html]] */
package object views {
  val logger = LogManager.getLogger("views")
}