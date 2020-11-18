package controllers

import deductions.runtime.core.HTTPrequest

import play.api.mvc.Action
import play.api.mvc.Request

import scalaz._
import Scalaz._


import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import deductions.runtime.utils.Configuration
import deductions.runtime.services.RecoverUtilities
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.html.TriplesViewWithTitle
import deductions.runtime.jena.RDFStoreLocalJenaProvider

class WebPagesTechApp @Inject() (
     components: ControllerComponents, configuration: play.api.Configuration)
extends { override implicit val config = new PlayDefaultConfiguration(configuration) }
with AbstractController(components)
  with WebPagesTech
  with HTMLGenerator


/** controller for Web Pages, Technical users */
trait WebPagesTech extends play.api.mvc.BaseController
with RDFStoreLocalJenaProvider
with RecoverUtilities[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with HTTPrequestHelpers
with LanguageManagement
with TriplesViewWithTitle[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
with HTTPoutputFromThrowable[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
{
  val config: Configuration
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
            Ok(htmlForm(uri, blankNode, editable = Edit  =/=  "", formuri,
              graphURI = makeAbsoluteURIForSaving(userid), database = database,
              HTTPrequest( acceptLanguages=Seq(lang) ) )._1)
              .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "*")
              .withHeaders(ACCESS_CONTROL_ALLOW_METHODS -> "*")
              .as("text/html; charset=utf-8")
          },
          (t: Throwable) =>
            errorResultFromThrowable(t, s"in /form?uri=$uri", request))
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
