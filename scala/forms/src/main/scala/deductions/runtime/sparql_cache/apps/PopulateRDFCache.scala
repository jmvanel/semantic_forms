package deductions.runtime.sparql_cache.apps

import deductions.runtime.DependenciesForApps
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.{CommonVocabulariesLoader, FormSpecificationsLoader}
import deductions.runtime.utils.DefaultConfiguration
import org.w3.banana.RDF

/**
 * Populate RDF Cache with commonly used vocabularies, form Specifications and I18N Translations;
 *  should be done just once (anyway because of SPARQL cache it goes quickly the second time);
 *
 *  To remove all such content, run [[deductions.runtime.sparql_cache.apps.ResetRDFCacheApp]] ;
 *
 *  similar to loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *
 *  TODO use prefix.cc web service to load from prefix short names (see implementation in EulerGUI)
 */
object PopulateRDFCache
  extends { override val config = new DefaultConfiguration{} }
  with DependenciesForApps  
  with PopulateRDFCacheTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]


trait PopulateRDFCacheTrait[Rdf <: RDF, DATASET]
    extends CommonVocabulariesLoader[Rdf, DATASET]
    with RDFI18NLoaderTrait[Rdf, DATASET]
    with FormSpecificationsLoader[Rdf, DATASET]
    with App {

  loadCommonVocabularies()
  resetCommonFormSpecifications()
  loadCommonFormSpecifications()
  loadFromGitHubRDFI18NTranslations()
}
