package controllers

import play.api.mvc.Request
import play.api.i18n.Lang
import java.util.Date
import java.text.DateFormat

/**
 * @author jmv
 */
trait LanguageManagement {
  
  def chooseLanguage(request: Request[_]): String = {
    chooseLanguageObject(request).language
  }

  def chooseLanguageObject(request: Request[_]): Lang = {
    val languages = request.acceptLanguages
    val resLang = if (languages.length > 0) languages(0) else Lang("en")
    println(
//    logger.info(
        DateFormat.getInstance.format(new Date()) +
        s" chooseLanguage: IP ${request.remoteAddress} $request\t$resLang, id ${request.id}, host ${request.host}")
    resLang
  }
  
}
