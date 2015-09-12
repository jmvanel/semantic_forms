package deductions.runtime.utils

/**
 * @author jmv
 */

import com.netaporter.i18n.ResourceBundle
import java.util.Locale

/** use https://github.com/NET-A-PORTER/scala-i18n */
object I18NMessages {
  val messages = ResourceBundle("messages.messages")

  def get(messageId: String, lang: String) = {
    val s = messages.getOrElse(messageId,
      Locale.forLanguageTag(lang),
      messageId)
    new String(s.getBytes("ISO-8859-1"), "UTF-8")
  }
}