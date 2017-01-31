package deductions.runtime

import org.apache.logging.log4j.LogManager

/** the HTML templates not for forms: page header, etc; other HTML Scala templates are in package [[html]] */
package object views {
  val logger = LogManager.getLogger("views")
}