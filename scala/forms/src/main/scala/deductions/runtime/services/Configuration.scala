package deductions.runtime.services

trait Configuration {
  /** vocabulary for form specifications */
  val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"

  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  val recordUserActions: Boolean = false

  // helper
  def needLogin = needLoginForEditing || needLoginForDisplaying
}