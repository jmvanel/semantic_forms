package deductions.runtime.sparql_cache

import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings

trait SitesURLForDownload {
  val githubcontent: String = "https://raw.githubusercontent.com"
}

/**
 * Populate RDF Cache with commonly used vocabularies;
 *  should be done just once (anyway because of SPARQL cache it goes quickly the second time);
 *
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *
 *  TODO use prefix.cc web service to load from prefix short names (see implementation in EulerGUI)
 *
 *  TODO move in jena package ( and update README )
 */
object PopulateRDFCache extends JenaModule
  //  with JenaHelpers
  with DefaultConfiguration
  with RDFStoreLocalJena1Provider
  with PopulateRDFCacheTrait[Jena, ImplementationSettings.DATASET]

trait PopulateRDFCacheTrait[Rdf <: RDF, DATASET]
    extends //    RDFOpsModule
    //    with
    CommonVocabulariesLoaderTrait[Rdf, DATASET]
    with RDFI18NLoaderTrait[Rdf, DATASET]
    with FormSpecificationsLoaderTrait[Rdf, DATASET]
    with App {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  loadCommonVocabularies()
  loadCommonFormSpecifications()
  loadFromGitHubRDFI18NTranslations()
}
