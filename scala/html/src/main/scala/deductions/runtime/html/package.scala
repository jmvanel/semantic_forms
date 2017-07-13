package deductions.runtime

import org.apache.logging.log4j.LogManager

/** the HTML generation for forms; other HTML Scala templates are in package [[views]] */
package object html {
  implicit val logger = LogManager.getLogger("html")
}