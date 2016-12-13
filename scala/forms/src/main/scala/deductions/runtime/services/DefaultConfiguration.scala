package deductions.runtime.services

import deductions.runtime.html.CSS

trait DefaultConfiguration extends Configuration {
  /**
   * use Local Host from getLocalHost() as Prefix For URI Creation, otherwise use defaultInstanceURIHostPrefix below
   *  TODO true should be the default, to adapt to normal machine settings
   */
  override val useLocalHostPrefixForURICreation = true // false

  /** URI Prefix prepended to newly created resource instances */
  override val defaultInstanceURIHostPrefix =
    "http://ldp.virtual-assembly.org"

  override val relativeURIforCreatedResourcesByForm = "ldp/"
  override val relativeURIforCreatedResourcesByLDP = relativeURIforCreatedResourcesByForm
  override val preferredURIPrefixes: Seq[String] = Seq("urn:av/")

  override def serverPort = {
    // println("Default port from DefaultConfiguration")
    "9000"
  }

  override val defaultReadTimeout = 10
  override val defaultConnectTimeout = 5
  override val httpHeadTimeout = 500

  //  override val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  // override val prefixAVontology = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"
  override val prefixAVontology = "http://www.virtual-assembly.org/ontologies/1.0/pair#"

  override val needLoginForEditing: Boolean = // false //
    true
  override val needLoginForDisplaying: Boolean = false

  override val recordUserActions: Boolean = true // false
  override val showDomainlessProperties = false
  override val addRDFS_label_comment = true

  override val lookup_domain_unionOf = false // is slow !!!

  override val use_dbpedia_lookup = true
  override val use_local_lookup = false
  /** show triples with rdf:type */
  override val showRDFtype = true
  /** show + Buttons for creating multi-valued */
  override val showPlusButtons = true
  /** show EDIT Buttons for multi-line editing popup */
  override val showEditButtons = true
  /** show all 3 buttons beside the current items in display or edit mode */
  override val showExpertButtons = true
  override val groupFields: Boolean = false // true

  override val inlineJavascriptInForm: Boolean = true
  override def displayTechnicalSemWebDetails: Boolean = true

  /** use Text indexing with Lucene or SOLR */
  override def useTextQuery: Boolean = true
  // false // 

  /** when #useTextQuery is true, use Text indexing with SOLR */
  override def solrIndexing: Boolean = false

  override val css: CSS = new CSS {}
  override val radioForIntervals = false

  //  override val activateUserInputHistory = false

  /** CORS */
  override val allow_Origin = "*"

  // relative URI's
  // maybe TODO use inverse Play's URI API
  override val hrefDisplayPrefix = "/display?displayuri="
  override val hrefDownloadPrefix = "/download?url="
  override val hrefEditPrefix = "/edit?url="
}
