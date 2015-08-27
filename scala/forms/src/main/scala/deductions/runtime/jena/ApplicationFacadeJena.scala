package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import com.hp.hpl.jena.query.Dataset

import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.ApplicationFacadeInterface

/**
 * ApplicationFacade for Jena
 *  TODO should not expose Jena, just ApplicationFacadeInterface
 */
trait ApplicationFacadeJena
    extends ApplicationFacadeInterface
    with ApplicationFacade[Jena, Dataset] {

  /**  NOTE: important that JenaModule is first; otherwise ops may be null */
  class ApplicationFacadeImplJena extends ApplicationFacadeImpl[Jena, Dataset]
      with JenaModule with RDFStoreLocalJena1Provider {

    val rdfStoreApp = new RDFStoreLocalJena2Provider {}
    import ops._
    val passwordsGraph = rdfStoreApp.rdfStore.getGraph(rdfStoreApp.dataset, URI("urn:users")).get
  }
  val facade = new ApplicationFacadeImplJena
}