package deductions.runtime.sparql_cache

object RDFI18NLoader extends RDFCache with App {
  loadFromGitHubRDFI18NTranslations

  def loadFromGitHubRDFI18NTranslations() {

    /* TODO : do not hardcode the URL's but read:
     * https://raw.githubusercontent.com/jmvanel/rdf-i18n/blob/master/translations_list.ttl
     * TODO use code to load all languages from rdf-i18n (see implementation in EulerGUI)
     * */
    val translations0 = List(
      "https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl",
      "https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.it.ttl",
      "https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.tr.ttl",
      "https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.fr.ttl",
      "https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.it.ttl")
    import ops._
    val translations = translations0 map { p => URI(p) }
    translations map { storeURI(_, dataset) }
  }
}