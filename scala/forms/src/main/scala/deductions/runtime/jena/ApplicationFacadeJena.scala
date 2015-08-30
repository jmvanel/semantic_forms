package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.ApplicationFacadeInterface
import deductions.runtime.dataset.RDFStoreLocalUserManagement

/**
 * ApplicationFacade for Jena
 *  TODO should not expose Jena, just ApplicationFacadeInterface
 */
trait ApplicationFacadeJena
    extends ApplicationFacadeInterface
    with ApplicationFacade[Jena, Dataset] {

  val facade = try {
    /**
     * NOTES:
     * - mandatory that JenaModule is first; otherwise ops may be null
     * - mandatory that RDFStoreLocalJena1Provider is before ApplicationFacadeImpl;
     *   otherwise allNamedGraph may be null
     */
    class ApplicationFacadeImplJena extends JenaModule
      with ApplicationFacadeImpl[Jena, Dataset]
      with RDFStoreLocalJena1Provider
      with RDFStoreLocalUserManagement[Jena, Dataset]

    new ApplicationFacadeImplJena

  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
}
