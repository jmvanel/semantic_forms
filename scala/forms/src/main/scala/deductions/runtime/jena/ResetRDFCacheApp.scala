package deductions.runtime.jena

import org.w3.banana.RDFOpsModule
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import deductions.runtime.sparql_cache.CommonVocabulariesLoaderTrait
import deductions.runtime.sparql_cache.FormSpecificationsLoaderTrait
import deductions.runtime.sparql_cache.RDFI18NLoaderTrait
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
object ResetRDFCacheApp extends ImplementationSettings.RDFModule
    with DefaultConfiguration
    with CommonVocabulariesLoaderTrait[Jena, ImplementationSettings.DATASET]
    with RDFI18NLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormSpecificationsLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFOpsModule
    with ImplementationSettings.RDFCache
    with App {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  resetCommonVocabularies()
  resetCommonFormSpecifications()
  resetRDFI18NTranslations()
}
