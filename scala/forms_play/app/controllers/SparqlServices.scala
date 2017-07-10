package controllers

import play.api.mvc._
//import java.nio.file.Files

//object Global extends GlobalSettings with Results {
//  override def onBadRequest(request: RequestHeader, error: String) = {
//    Future{ BadRequest("""Bad Request: "$error" """) }
//  }
//}

/** main controller 
 *  TODO split HTML pages & HTTP services */
trait SparqlServices extends ApplicationTrait
{
  import config._
  /** load RDF String in database - TODO conneg !!! */
  def loadAction() //  data: String, graphURI: String = "",
  //                 database: String = "TDB") 
  = Action {
    implicit request: Request[AnyContent] =>
      val requestCopy = getRequestCopy()
      println(s"body class ${request.getClass} ${request.body}")     
      val content = getContent(request)
      println(s"content ${content.toString.substring(0, 50) + " ..."}")
      load(requestCopy.copy(content = content))
      Ok("OK")
  }
  def updateGET(updateQuery: String): EssentialAction = update(updateQuery)
  def updatePOST(): EssentialAction = update("")

  /** SPARQL Construct UI */
  def sparql(query: String) = {
    logger.info("sparql: " + query)
    def doAction(implicit request: Request[_]) = {
      logger.info("sparql: " + request)
      val lang = chooseLanguage(request)
      outputMainPage(sparqlConstructQuery(query, lang), lang)

        // TODO factorize
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
        .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
    }
    if (needLoginForDisplaying)
      Action { implicit request: Request[_] => doAction }
    else
      withUser { implicit userid => implicit request => doAction }
  }
}
