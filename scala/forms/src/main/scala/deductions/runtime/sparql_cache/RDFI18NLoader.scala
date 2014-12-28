package deductions.runtime.sparql_cache

object RDFI18NLoader extends RDFCache with App {
  def loadFromGitHubRDFI18NTranslations() {

    /* TODO : do not hardcode the URL's but read
     * https://raw.githubusercontent.com/jmvanel/rdf-i18n/blob/master/translations_list.ttl */
    val translations0 = List(
      "http://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl",
      "http://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.it.ttl",
      "http://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.tr.ttl",
      "http://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.fr.ttl",
      "http://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.it.ttl")
    import ops._
    val translations = translations0 map { p => URI(p) }
    translations map { storeURI(_, dataset) }
  }
}