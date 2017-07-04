package deductions.runtime.sparql_cache.apps

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.{CommonVocabulariesLoaderTrait, FormSpecificationsLoaderTrait}
import org.w3.banana.RDFOpsModule

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
object ResetRDFCacheApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFModule
    with DefaultConfiguration
    with CommonVocabulariesLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFI18NLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with FormSpecificationsLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFOpsModule
    with ImplementationSettings.RDFCache
    with App {

  resetCommonVocabularies()
  resetCommonFormSpecifications()
  resetRDFI18NTranslations()
}
