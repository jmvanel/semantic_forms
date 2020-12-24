package controllers

import play.api.i18n.Lang
import play.api.mvc.Request

/**
 * @author jmv
 */
trait LanguageManagement extends RequestUtils {
  
  def chooseLanguage(request: Request[_]): String = {
    chooseLanguageObject(request).language
  }

  def chooseLanguageObject(request: Request[_]): Lang = {
    val languages = request.acceptLanguages
    val resLang = if (languages.length > 0) languages(0) else Lang("en")
    logger.debug(
        log("chooseLanguage", request) + s"\t$resLang" )
    resLang
  }
  
}
