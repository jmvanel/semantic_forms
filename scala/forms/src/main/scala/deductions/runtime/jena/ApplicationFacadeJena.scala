package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.dataset.RDFStoreLocalUserManagement
import deductions.runtime.services.ApplicationFacade
import deductions.runtime.services.ApplicationFacadeImpl
import deductions.runtime.services.ApplicationFacadeInterface
import org.w3.banana.URIOps
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.services.DefaultConfiguration
import scala.xml.NodeSeq

/**
 * ApplicationFacade for Jena,
 * does not expose Jena, just ApplicationFacadeInterface
 */
trait ApplicationFacadeJena
    extends ApplicationFacadeInterface
    with ApplicationFacade[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJenaProvider {
  override val impl = try {
    /**
     * NOTES:
     * - mandatory that JenaModule is first; otherwise ops may be null
     * - mandatory that RDFStoreLocalJena1Provider is before ApplicationFacadeImpl;
     *   otherwise allNamedGraph may be null
     */
    class ApplicationFacadeImplJena extends JenaModule
      with RDFStoreLocalJena1Provider
      with DefaultConfiguration
      with ApplicationFacadeImpl[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
      with RDFStoreLocalUserManagement[ImplementationSettings.Rdf, ImplementationSettings.DATASET]

    new ApplicationFacadeImplJena {
      override def htmlForm(uri0: String, blankNode: String = "",
        editable: Boolean = false,
        lang: String = "en", formuri: String = ""): NodeSeq = {
        println(s""">> ApplicationFacadeImplJena 
                max  Memory  ${Runtime.getRuntime.maxMemory()}
                totalMemory  ${Runtime.getRuntime.totalMemory()}""")
        val name = "TDB/journal.jrnl"
        println(s"$name  : ${new java.io.File(name).length()} bytes")
        //                dataset.asInstanceOf[com.hp.hpl.jena.query.Dataset].getContext.
        //                com.hp.hpl.jena.tdb.transaction.TransactionManager.DEBUG
        //        dataset.close()
        //        dataset = createDatabase( databaseLocation )
        super.htmlForm(uri0: String, blankNode,
          editable, lang: String)
      }
    }
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
}
