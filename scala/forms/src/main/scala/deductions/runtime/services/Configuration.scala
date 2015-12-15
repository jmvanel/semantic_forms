package deductions.runtime.services

trait Configuration {
  /** URI Prefix prepended to newly created resource instances */
  var defaultInstanceURIHostPrefix = "http://assemblee-virtuelle.org/"
  /** otherwise use defaultInstanceURIHostPrefix */
  var useLocalHostPrefixForURICreation = false

  val relativeURIforCreatedResourcesByForm = "ldp/"
  val relativeURIforCreatedResourcesByLDP = relativeURIforCreatedResourcesByForm

  /** vocabulary for form specifications */
  val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  val prefixAVontology = "http://www.assemblee-virtuelle.org/ontologies/v1.owl#"

  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  val recordUserActions: Boolean = false
  val showDomainlessProperties = false
  val addRDFS_label_comment = true

  val lookup_domain_unionOf = true // false

  val use_dbpedia_lookup = false
  val use_local_lookup = true

//  val activateUserInputHistory = false
  
  // CORS
  val allow_Origin = "*"

  // relative URI's
  // maybe TODO use inverse Play's URI API
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="
  val hrefEditPrefix = "/edit?url="

  // helper functions
  def needLogin = needLoginForEditing || needLoginForDisplaying
}