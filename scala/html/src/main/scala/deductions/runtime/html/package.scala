package deductions.runtime

import org.apache.logging.log4j.LogManager
import deductions.runtime.utils.I18NMessages
import deductions.runtime.core.HTTPrequest

/** the HTML generation for forms; other HTML Scala templates are in package [[views]] */
package object html {
  implicit val logger = LogManager.getLogger("html")

  def message(m: String, lang: String): String = I18NMessages.get(m, lang)
  def messageRequest(m: String, request: HTTPrequest): String = I18NMessages.get(m, request.getLanguage())

  def mess(m: String)(implicit lang: String) = message(m, lang)
  def messRequest(m: String)(implicit request: HTTPrequest) = message(m, request.getLanguage())

}