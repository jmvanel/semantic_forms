package controllers.semforms.services

import play.api.mvc.Request

trait ApplicationCommons {

	def chooseLanguage(request: Request[_]): String = {
    val languages = request.acceptLanguages
    val res = if (languages.length > 0) languages(0).language else "en"
    println("chooseLanguage: " + request + "\n\t" + res)
    res
  }
}