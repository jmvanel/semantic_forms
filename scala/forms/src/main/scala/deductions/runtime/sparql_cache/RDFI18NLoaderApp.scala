package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration

/** TODO put in package jena */
object RDFI18NLoaderApp
    extends ImplementationSettings.RDFModule
    with RDFCache with App
    with RDFI18NLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFStoreLocalJena1Provider {
  val config = new DefaultConfiguration {}
  loadFromGitHubRDFI18NTranslations()
}

trait RDFI18NLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  val i18NGraph = URI("urn:rdf-i18n")

  /** TRANSACTIONAL */
  def resetRDFI18NTranslations() {
    val r = dataset.rw({
      rdfStore.removeGraph( dataset, i18NGraph)
    })
  }

  /** load RDF I18N Translations From GitHub, into named graph "rdf-i18n";
   *  TRANSACTIONAL */
  def loadFromGitHubRDFI18NTranslations() {

    /* TODO : do not hardcode the URL's but read:
     * https://raw.githubusercontent.com/jmvanel/rdf-i18n/blob/master/translations_list.ttl
     * TODO use code to load all languages from rdf-i18n (see implementation in EulerGUI)
     * */

    val translations0 = List(
      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.it.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.tr.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/rdfs/rdfs.fr.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/rdfs/rdfs.it.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/rdf/rdf.fr.ttl",
      s"${githubcontent}/jmvanel/rdf-i18n/master/contact/contact.fr.ttl"
    )
    import ops._
    val translations = translations0 map { p => URI(p) }
    translations map { storeURI(_, i18NGraph, dataset) }
  }
}