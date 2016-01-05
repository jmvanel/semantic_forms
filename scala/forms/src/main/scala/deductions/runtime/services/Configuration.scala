package deductions.runtime.services

//trait Configuration {
trait Configuration {
  /** URI Prefix prepended to newly created resource instances */
  def defaultInstanceURIHostPrefix = "http://assemblee-virtuelle.org/"
  /** otherwise use defaultInstanceURIHostPrefix */
  def useLocalHostPrefixForURICreation = false

  def relativeURIforCreatedResourcesByForm = "ldp/"
  def relativeURIforCreatedResourcesByLDP = relativeURIforCreatedResourcesByForm

  /** vocabulary for form specifications */
  def formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  def prefixAVontology = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"

  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  def recordUserActions: Boolean = false
  def showDomainlessProperties = false
  def addRDFS_label_comment = true

  def lookup_domain_unionOf = false // is slow !!!

  def use_dbpedia_lookup = true
  def use_local_lookup = false
  /** show triples with rdf:type */
  def showRDFtype = true
  /** show + Buttons for creating multi-valued */
  def showPlusButtons: Boolean // = true

  //  def radioForIntervals = false // TODO

  //  def activateUserInputHistory = false

  // CORS
  def allow_Origin = "*"

  // relative URI's
  // maybe TODO use inverse Play's URI API
  def hrefDisplayPrefix = "/display?displayuri="
  def hrefDownloadPrefix = "/download?url="
  def hrefEditPrefix = "/edit?url="

  // helper functions
  def needLogin = needLoginForEditing || needLoginForDisplaying
}