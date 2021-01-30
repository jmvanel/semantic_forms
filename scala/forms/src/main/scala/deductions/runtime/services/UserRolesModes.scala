package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import scala.xml.NodeSeq
import scala.io.Source
import java.util.Properties
import java.io.FileReader
import scala.util.Try
import deductions.runtime.services.UserRoles.UserRole
import deductions.runtime.services.UserRoles.ContentManager

/** cf https://github.com/jmvanel/semantic_forms/issues/209 */
object UserRolesModes {

  /** apply App User Mode defined by config.file
   *  @param defaultResponse HTTP request response, unfiltered */
  def applyAppUserMode[RESP](req: HTTPrequest, defaultResponse: RESP): RESP = {
    getAppUserMode.
      httpRequestTriage(req, defaultResponse)
  }

  sealed trait AppUserMode {
    def httpRequestTriage[RESP](req: HTTPrequest, defaultResponse: RESP): RESP = {
      defaultResponse
    }
  }

  /** historic, default mode: any logged user can /create and /edit 
   *  her triples */
  case object EditAllowed extends AppUserMode

  /** Only administrator can update the site */
  case object EditByAdmin extends AppUserMode {
    def httpRequestTriage(req: HTTPrequest, defaultResponse: NodeSeq): NodeSeq =
      httpRequestTriageAdmin(req, defaultResponse)
  }

  def getUserRole(req: HTTPrequest): UserRole = ???

  /** Only ContentManager can update the site with /edit, /create,
   *  /load,  /load-uri , etc ;
   *  but TODO cannot create other ContentManager's ;
   *  only admin can ;
   *  PENDING: will ContentManager be allowed /update ?
   *  */
  case object EditByContentManagers extends AppUserMode{
    def httpRequestTriage(req: HTTPrequest, defaultResponse: NodeSeq): NodeSeq =
      httpRequestTriageAdmin(req, defaultResponse)
  }

  /**  logged user can propose content, which ContentManager will approve, thus updating the site
   *   TODO */
  case object CMSmode extends AppUserMode

  def httpRequestTriageAdmin(
    req:             HTTPrequest,
    defaultResponse: scala.xml.NodeSeq): scala.xml.NodeSeq = {
    val path = req.path
    val method = req.method
    val isChangeBearingsRoute = changeBearingsRoutes.contains(path)
    if (req.userId() == "admin" ||
      !isChangeBearingsRoute ||
      (isChangeBearingsRoute && method == "GET"))
      defaultResponse
    else
      <p>Not allowed, need to be admin or Content Manager.</p>
  }
  
  private val changeBearingsRoutes = Set(
    "/load-uri",

    "/load",

    "/edit",
    "/save",
    "/create",

    "/create-data",
    "/sparql-data",

    "/page",

    "/update",

    "/login",
    "/authenticate",
    "/register",
    "/logout")
  /** read file or system property for application mode */
  def getAppUserMode: AppUserMode = {
    val props = new Properties
    Try {
      props.load(new FileReader("app.properties"))
      import scala.collection.JavaConverters._
      getAppUserModeFromString(
        props.asScala.getOrElse("AppUserMode", "EditAllowed"))
    } . getOrElse(EditAllowed)
  }


  private val appUserModeMap = Map(
      "EditAllowed" -> EditAllowed,
      "EditByContentManagers" -> EditByContentManagers,
      "CMSmode" -> CMSmode
      )

   private def getAppUserModeFromString(s: String): AppUserMode = {
    appUserModeMap.getOrElse(s, EditAllowed)
  }

}