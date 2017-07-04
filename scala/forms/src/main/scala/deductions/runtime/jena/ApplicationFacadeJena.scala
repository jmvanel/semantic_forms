package deductions.runtime.jena

import deductions.runtime.dataset.RDFStoreLocalUserManagement
//import deductions.runtime.services.ApplicationFacade
import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.utils.Configuration

/**
 * Application Facade implemented with Jena,
 * but does not expose Jena nor Banana, just ApplicationFacadeInterface
 */
trait ApplicationFacadeJena
    extends ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    //cwith ApplicationFacadeInterface
    with RDFStoreLocalJenaProvider {

  /** These 2 dependencies are transmitted below to the actual running instance `impl` */
  val config: Configuration
  val htmlGenerator: HtmlGeneratorInterface[Rdf#Node, Rdf#URI]

  // TODO how to avoid these assignments ?
  val conf = config
  val ops1 = ops
  lazy val htmlGenerator2 = htmlGenerator

   val impl: ApplicationFacadeImpl[Rdf, DATASET] =
    try {
      /**
       * NOTES:
       * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
       * - mandatory that RDFStoreLocalJena1Provider is before ApplicationFacadeImpl;
       *   otherwise allNamedGraph may be null
       */
      abstract class ApplicationFacadeImplJena
        extends {
        override val config = conf
        override val htmlGenerator = htmlGenerator2
      } with ImplementationSettings.RDFModule
        with RDFStoreLocalJenaProvider
        with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
        with RDFStoreLocalUserManagement[ImplementationSettings.Rdf, ImplementationSettings.DATASET]

      new ApplicationFacadeImplJena {}

    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
}
