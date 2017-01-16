package deductions.runtime.jena

import scala.xml.NodeSeq

import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.ApplicationFacadeInterface
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest

/**
 * ApplicationFacade implemeted with Jena,
 * but does not expose Jena nor Banana, just ApplicationFacadeInterface
 */
trait ApplicationFacadeJena
    extends ApplicationFacadeInterface
    with ApplicationFacade[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJenaProvider {

  val config: Configuration
  val conf = config
  //  def getRequest(): HTTPrequest
  //  val getRequest1: () => HTTPrequest

  override val impl: ApplicationFacadeImpl[Rdf, DATASET] = try {
    /**
     * NOTES:
     * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
     * - mandatory that RDFStoreLocalJena1Provider is before ApplicationFacadeImpl;
     *   otherwise allNamedGraph may be null
     */
    abstract class ApplicationFacadeImplJena
      extends { override val config = conf } with ImplementationSettings.RDFModule
      with RDFStoreLocalJenaProvider
      with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
      with RDFStoreLocalUserManagement[ImplementationSettings.Rdf, ImplementationSettings.DATASET]

    new ApplicationFacadeImplJena {

      //      override def getRequest: HTTPrequest = getRequest1()

      //      /** Overridden just for some logging */
      //      override def htmlForm(uri0: String, blankNode: String = "",
      //        editable: Boolean = false,
      //        lang: String = "en", formuri: String = "",
      //        graphURI: String = "", database: String = "TDB"): NodeSeq = {
      //        println(s""">> ApplicationFacadeImplJena 
      //                max  Memory  ${Runtime.getRuntime.maxMemory()}
      //                totalMemory  ${Runtime.getRuntime.totalMemory()}""")
      //        val name = "TDB/journal.jrnl"
      //        println(s"$name  : ${new java.io.File(name).length()} bytes")
      //        super.htmlForm(uri0: String, blankNode,
      //          editable, lang: String, graphURI = graphURI, database = database)
      //      }
    }
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
}
