package controllers

import deductions.runtime.core.HTTPrequest

import play.api.mvc.Action
import play.api.mvc.Request

import scalaz._
import Scalaz._


class WebPagesTechApp extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  with WebPagesTech
  with HTMLGenerator


/** controller for Web Pages, Technical users */
trait WebPagesTech extends PlaySettings.MyControllerBase
with ApplicationTrait
{
  import config._

  /** "naked" HTML form */
  def form(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "",
           database: String = "TDB") =
    Action {
      implicit request: Request[_] =>
        recoverFromOutOfMemoryErrorGeneric(
          {
            logger.info(s"""form: request $request : "$Edit" formuri <$formuri> """)
            val lang = chooseLanguage(request)
            val requestCopy = getRequestCopy()
            val userid = requestCopy.userId()
            Ok(htmlForm(uri, blankNode, editable = Edit  =/=  "", lang, formuri,
              graphURI = makeAbsoluteURIForSaving(userid), database = database, HTTPrequest() )._1)
              .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
              .as("text/html; charset=utf-8")
          },
          (t: Throwable) =>
            errorResultFromThrowable(t, s"in /form?uri=$uri"))
    }
  
//    /** output Main Page With given Content */
//  private def outputMainPageWithContent(contentMaker: SemanticController, classForContent: String = "") = {
//    Action { request0: Request[_] =>
//      val precomputed = new MainPagePrecompute(request0)
//      import precomputed._
//      //        println(s"========= outputMainPageWithContent precomputed $precomputed - title ${precomputed.title}")
//      addAppMessageFromSession(requestCopy)
//      outputMainPage2(
//        contentMaker.result(requestCopy),
//        precomputed, classForContent = classForContent)
//    }
//  }

  ///////////////// SPARQL related ////////////////////////
  
//    /**
//   * /sparql-form service: Create HTML form or view from SPARQL (construct);
//   *  like /sparql has input a SPARQL query;
//   *  like /form and /display has parameters Edit, formuri & database
//   */
//  def sparqlForm(query: String, Edit: String = "", formuri: String = "",
//                 database: String = "TDB") =
//    Action { implicit request: Request[_] =>
//      recoverFromOutOfMemoryErrorGeneric(
//        {
//          val requestCopy = getRequestCopy()
//          val userid = requestCopy.userId()
//          val lang = chooseLanguage(request)
//          val userInfo = displayUser(userid, "", "", lang)
//          outputMainPage(
//            createHTMLFormFromSPARQL(
//              query,
//              editable = Edit  =/=  "",
//              formuri, requestCopy),
//            lang, userInfo, classForContent="sf-complete-form")
//        },
//        (t: Throwable) =>
//          errorResultFromThrowable(t, "in /sparql-form"))
//    }
}