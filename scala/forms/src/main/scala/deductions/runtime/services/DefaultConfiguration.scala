package deductions.runtime.services

import deductions.runtime.html.CSS

//trait Configuration {
trait DefaultConfiguration extends Configuration {
  /** URI Prefix prepended to newly created resource instances */
  override val defaultInstanceURIHostPrefix =
    "http://ldp.assemblee-virtuelle.org/" // ?????? 
  /** otherwise use defaultInstanceURIHostPrefix */
  override val useLocalHostPrefixForURICreation = false

  override val relativeURIforCreatedResourcesByForm = "ldp/"
  override val relativeURIforCreatedResourcesByLDP = relativeURIforCreatedResourcesByForm

  /** vocabulary for form specifications */
  override val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  override val prefixAVontology = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"

  override val needLoginForEditing: Boolean = false // true
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
  //  lazy override val showPlusButtons = true
  override val showPlusButtons = true
  override val inlineJavascriptInForm: Boolean = true
  override def displayTechnicalSemWebDetails: Boolean = true

  /** use Text indexing with Lucene or SOLR */
  override def useTextQuery: Boolean = false // true
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