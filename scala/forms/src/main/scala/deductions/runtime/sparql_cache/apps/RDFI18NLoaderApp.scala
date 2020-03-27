package deductions.runtime.sparql_cache.apps

import deductions.runtime.DependenciesForApps
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.sparql_cache.{RDFCacheAlgo, SitesURLForDownload}
import deductions.runtime.utils.{DefaultConfiguration, RDFHelpers}
import org.w3.banana.{Prefix, RDF}

/** */
object RDFI18NLoaderApp
    extends { override val config = new DefaultConfiguration {} } with DependenciesForApps
    with RDFI18NLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  resetRDFI18NTranslations()
  loadFromGitHubRDFI18NTranslations()
}


trait RDFI18NLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with SitesURLForDownload {

  import ops._

  val i18NGraph = URI("urn:rdf-i18n")

  /** TRANSACTIONAL */
  def resetRDFI18NTranslations() {
    val r = rdfStore.rw( dataset, {
      rdfStore.removeGraph( dataset, i18NGraph)
    })
  }

  /** load RDF I18N Translations From GitHub, into named graph "rdf-i18n";
   *  TRANSACTIONAL */
  def loadFromGitHubRDFI18NTranslations() {

    /* do not hardcode the URL's but read:
     * https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/translations_list.ttl
     * and query ?S lingvoj:translatedResource ?RES */
	  val translationDocURL = s"${githubcontent}/jmvanel/rdf-i18n/master/translations_list.ttl"

     val translationDoc = rdfLoader.load(new java.net.URL(translationDocURL)) . getOrElse (sys.error(
          s"couldn't read translation Doc URL <$translationDocURL>"))

     println(s"translationDoc $translationDoc" )

     val lingvoj = Prefix[Rdf]( "lingvoj", "http://www.lingvoj.org/ontology#")
     val trtr = find( translationDoc, ANY, lingvoj("translatedResource"), ANY )
     val translations1 = trtr . map { tr => tr.objectt } . toList
     println(s"trtr $trtr" )

    //   val translations0 = List(
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.it.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/foaf/foaf.tr.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/rdfs/rdfs.fr.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/rdfs/rdfs.it.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/rdf/rdf.fr.ttl",
    //      s"${githubcontent}/jmvanel/rdf-i18n/master/contact/contact.fr.ttl"
    //    )
    //    val translations = translations0 map { p => URI(p) }
    translations1.map {
      uri =>
        println(s"BEFORE loading <$uri> into <$i18NGraph>")
        readStoreURI(uriNodeToURI(uri), i18NGraph, dataset)
        println(s"DONE loaded    <$uri> into <$i18NGraph>")
    }
  }
}