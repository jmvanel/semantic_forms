package deductions.runtime.jena

import scala.xml.NodeSeq

import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.ApplicationFacadeInterface
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.html.Form2HTMLBanana
import org.w3.banana.RDFOps

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
  val ops1 = ops

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
      val htmlGenerator = new Form2HTMLBanana[ImplementationSettings.Rdf] {
        implicit val ops: RDFOps[ImplementationSettings.Rdf] = ops1
        val config = conf
        val nullURI = ops.URI("")
      }
    }

  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
}
