package deductions.runtime.jena

import org.w3.banana.RDFOpsModule
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.sparql_cache.CommonVocabulariesLoaderTrait
import deductions.runtime.sparql_cache.FormSpecificationsLoaderTrait
import deductions.runtime.sparql_cache.RDFI18NLoaderTrait
import org.w3.banana.jena.JenaModule
import deductions.runtime.services.DefaultConfiguration

/**
 * Populate RDF Cache with commonly used vocabularies;
 *  should be done just once;
 *
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *
 *  TODO use prefix.cc web service to load from prefix short names (see implementation in EulerGUI)
 */
object ResetRDFCache extends JenaModule
    with DefaultConfiguration
    with CommonVocabulariesLoaderTrait[Jena, Dataset]
    with RDFI18NLoaderTrait[Jena, Dataset]
    with FormSpecificationsLoaderTrait[Jena, Dataset]
    with RDFOpsModule
    with RDFStoreLocalJena1Provider
    //    with JenaHelpers
    with App {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  resetCommonVocabularies()
  resetCommonFormSpecifications()
  resetRDFI18NTranslations()
}
