package sf {

import deductions.runtime.jena.RDFStoreLocalJenaProvider
import org.w3.banana.jena.JenaModule
import deductions.runtime.utils.DefaultConfiguration

  /** Can be used e.g. for running in Scala console:
   *
   * sf.RDFStoreJena.closeAllTDBs() */
  object RDFStoreJena extends JenaModule
    with RDFStoreLocalJenaProvider {
    val config = new DefaultConfiguration {
      override val useTextQuery = false
    }
  }
}