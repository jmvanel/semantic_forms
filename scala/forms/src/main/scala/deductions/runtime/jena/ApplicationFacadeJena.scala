package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import com.hp.hpl.jena.query.Dataset

import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl

/** ApplicationFacade for Jena */
trait ApplicationFacadeJena extends ApplicationFacade[Jena, Dataset] {
  /**  NOTE: important that JenaModule is first; otherwise ops may be null */
  class ApplicationFacadeImplJena extends ApplicationFacadeImpl[Jena, Dataset]
    with JenaModule with RDFStoreLocalJena1Provider
  val facade = new ApplicationFacadeImplJena
}