package deductions.runtime.sparql_cache

import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFCache
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset

object RDFI18NLoader extends RDFCache with App
with RDFI18NLoaderTrait[Jena, Dataset]
    with RDFStoreLocalJena1Provider
    with JenaHelpers {
  loadFromGitHubRDFI18NTranslations()
}

trait RDFI18NLoaderTrait[Rdf <: RDF, DATASET]
extends RDFCacheAlgo[Rdf, DATASET]
    with RDFStoreHelpers[Rdf, DATASET]
    with SitesURLForDownload
{

  /** load RDF I18N Translations From GitHub, into named graph "rdf-i18n" */
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
    translations map { storeURI(_, URI("rdf-i18n"), dataset) }
  }
}