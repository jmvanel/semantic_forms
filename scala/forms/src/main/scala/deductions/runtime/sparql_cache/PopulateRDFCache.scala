package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.DependenciesForApps

trait SitesURLForDownload {
  val githubcontent: String = "https://raw.githubusercontent.com"
}

/**
 * Populate RDF Cache with commonly used vocabularies, formS pecifications and I18N Translations;
 *  should be done just once (anyway because of SPARQL cache it goes quickly the second time);
 *
 *  To remove all such content, run [[deductions.runtime.jena.ResetRDFCacheApp]] ;
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
    extends CommonVocabulariesLoaderTrait[Rdf, DATASET]
    with RDFI18NLoaderTrait[Rdf, DATASET]
    with FormSpecificationsLoaderTrait[Rdf, DATASET]
    with App {

  loadCommonVocabularies()
  loadCommonFormSpecifications()
  loadFromGitHubRDFI18NTranslations()
}
