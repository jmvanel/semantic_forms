package deductions.runtime.services

trait Configuration {
  /** URI Prefix prepended to newly created resource instances */
  var defaultInstanceURIPrefix = "http://assemblee-virtuelle.org/resource/"

  /** vocabulary for form specifications */
  val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  val recordUserActions: Boolean = false

  // helper
  def needLogin = needLoginForEditing || needLoginForDisplaying
}