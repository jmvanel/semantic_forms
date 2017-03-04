package controllers

import play.api.mvc.Request
import play.api.i18n.Lang

/**
 * @author jmv
 */
trait LanguageManagement {
  
  def chooseLanguage(request: Request[_]): String = {
    chooseLanguageObject(request).language
  }

  def chooseLanguageObject(request: Request[_]): Lang = {
    val languages = request.acceptLanguages
    val res = if (languages.length > 0) languages(0) else Lang("en")
    logger.info("chooseLanguage: " + request + "\t" + res)
    res
  }
  
}
