package deductions.runtime.utils

/**
 * @author jmv
 */

import com.netaporter.i18n.ResourceBundle
import java.util.Locale
import java.text.MessageFormat

/** use https://github.com/NET-A-PORTER/scala-i18n */
object I18NMessages {
  val messages = ResourceBundle("messages.messages")

  def get(messageId: String, lang: String) = {
    val language = if (lang == "") "en" else lang
    val s = messages.getOrElse(messageId,
      Locale.forLanguageTag(language), messageId)
    new String(s.getBytes("ISO-8859-1"), "UTF-8")
  }

  def format(messageId: String, lang: String, messageArguments: String*): String = {
    val language = if (lang == "") "en" else lang
    val loc = Locale.forLanguageTag(language)
    val template0 = messages.getOrElse(messageId,
      Locale.forLanguageTag(language), messageId)
    val template = new String(template0.getBytes("ISO-8859-1"), "UTF-8")

    val formatter = new MessageFormat("")
    formatter.setLocale(loc)
    // 5. Format the Message Using the Pattern and the Arguments
    /* This step shows how the pattern, message arguments, and formatter all work together. First,
     * fetch the pattern String from the ResourceBundle with the getString method.
     * The key to the pattern is template.
     * Pass the pattern String to the formatter with the applyPattern method.
     * Then format the message using the array of message arguments, 
     * by invoking the format method. The String returned by the format method is ready to be displayed.
     * All of this is accomplished with just two lines of code: */
    formatter.applyPattern(template)

    formatter.format( messageArguments.toList )
  }
}