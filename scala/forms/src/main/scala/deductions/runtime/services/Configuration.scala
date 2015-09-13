package deductions.runtime.services

trait Configuration {
  // Configuration
  def needLoginForEditing: Boolean = false // true
  def needLoginForDisplaying: Boolean = false

  // helper
  def needLogin = needLoginForEditing || needLoginForDisplaying
}