package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import scala.xml.NodeSeq
import scala.io.Source
import java.util.Properties
import java.io.FileReader
import scala.util.Try
import deductions.runtime.services.UserRoles.UserRole
import deductions.runtime.services.UserRoles.ContentManager
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration

/** cf https://github.com/jmvanel/semantic_forms/issues/209 */
object UserRolesModes extends   {
    override implicit val config = new DefaultConfiguration {}
  }
  with deductions.runtime.jena.RDFStoreLocalJenaProvider
   with RecoverUtilities
  [ImplementationSettings.Rdf, ImplementationSettings.DATASET]
{

//  def recoverFromOutOfMemoryErrorAapplyAppUserMode[RESP](req: HTTPrequest, defaultResponse: RESP): RESP = {
//    recoverFromOutOfMemoryErrorGeneric(
//        applyAppUserMode[RESP](req, defaultResponse),
//        (t: Throwable) => ()
//    )
//  }

  /** apply App User Mode defined by config.file
   *  @param defaultResponse HTTP request response, unfiltered */
  def applyAppUserMode[RESP](req: HTTPrequest, defaultResponse: RESP): RESP = {
    val mode = getAppUserMode
    logger.debug(s"mode '$mode'")
    mode.httpRequestTriage(req, defaultResponse)
  }

  sealed
  trait AppUserMode {
    def httpRequestTriage[RESP](req: HTTPrequest, defaultResponse: => RESP): RESP = {
      logger.debug(s"this '$this'")
      if (isRouteAllowedForUserMode(req)) {
        logger.debug(s"httpRequestTriage default Response $req")
        defaultResponse
      } else
        throw new Exception(
         notAllowedMessage(req) )
    }
    def isRouteAllowedForUserMode(req: HTTPrequest): Boolean
    def notAllowedMessage(req: HTTPrequest) = ""
  }

  /** historic, default mode: any logged user can /create and /edit 
   *  her triples */
  case object EditAllowed extends AppUserMode {
    override def isRouteAllowedForUserMode(req: HTTPrequest): Boolean = true
  }

  /** Only administrator can update the site */
  case object EditByAdmin extends AppUserMode {
    override def isRouteAllowedForUserMode(req: HTTPrequest): Boolean = isRouteAllowedInModeAdmin(req)
    override def notAllowedMessage(req: HTTPrequest) = "Not allowed, need to be admin or Content Manager."
  }

  /** Only ContentManager can update the site with /edit, /create,
   *  /load,  /load-uri , etc ;
   *  but TODO cannot create other ContentManager's ;
   *  only admin can ;
   *  PENDING: will ContentManager be allowed /update ?
   *  */
  case object EditByContentManagers extends AppUserMode {
    override def isRouteAllowedForUserMode(req: HTTPrequest): Boolean = isRouteAllowedInModeAdmin(req)
    override def notAllowedMessage(req: HTTPrequest) = "Not allowed, need to be admin or Content Manager."
  }

  /**  logged user can propose content, which ContentManager will approve, thus updating the site
   *   TODO */
  case object CMSmode extends AppUserMode {
    override def isRouteAllowedForUserMode(req: HTTPrequest): Boolean = true
    override def notAllowedMessage(req: HTTPrequest) = "Not allowed, need to be admin or Content Manager."
  }

  private def getUserRole(req: HTTPrequest): UserRole = ???

  /** is Route Allowed For any User in User Mode Admin ? */
  private def isRouteAllowedInModeAdmin(req: HTTPrequest): Boolean = {
    val path = req.path
    val method = req.method
    logger.debug(s"path '$path'")
    val isChangeBearingsRoute = changeBearingsRoutes.contains(path)
    logger.debug(s"isChangeBearingsRoute '$isChangeBearingsRoute' , method $method")
    req.userId == "admin" ||  // addmin can do anything
      !isChangeBearingsRoute || // anyone can do read only things
      (path.startsWith("/ldp") && method == "GET") // LDP POST and PUT are not for anyone
  }

  private val changeBearingsRoutes = Set(
    "/load-uri",
    "/load",

    "/edit",
    "/save",
    "/create",

//    "/create-data",
//    "/sparql-data",

//    "/page",

    "/update",
    "/update-ui",

//    "/login",
//    "/authenticate",
//    "/register",
//    "/logout"
  )

  /** read file or system property for application mode */
  private def getAppUserMode: AppUserMode = {
    val props = new Properties
    Try {
      props.load(new FileReader("app.properties"))
      import scala.jdk.CollectionConverters._
      getAppUserModeFromString(
        props.asScala.getOrElse("AppUserMode", "EditAllowed"))
    } . getOrElse(EditAllowed)
  }


  private val appUserModeMap = Map(
      "EditAllowed" -> EditAllowed,
      "EditByAdmin" ->  EditByAdmin,
      "EditByContentManagers" -> EditByContentManagers,
      "CMSmode" -> CMSmode
      )

   private def getAppUserModeFromString(s: String): AppUserMode = {
    appUserModeMap.getOrElse(s, EditAllowed)
  }

}
