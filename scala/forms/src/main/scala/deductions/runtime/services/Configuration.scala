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

  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  val recordUserActions: Boolean = false
  val showDomainlessProperties = false
  val addRDFS_label_comment = true

  // CORS
  val allow_Origin = "*"

  // helper functions
  def needLogin = needLoginForEditing || needLoginForDisplaying
}