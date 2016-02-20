package deductions.runtime.services

import deductions.runtime.html.CSS

/**
 * All Configuration flags and values for the library
 *  TODO should 100% abstract
 */
trait Configuration {
  // TODO all members should be abstract!!!!!!!!! 

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

  def recordUserActions: Boolean
  def showDomainlessProperties = false
  def addRDFS_label_comment: Boolean

  def lookup_domain_unionOf = false // is slow !!!

  def use_dbpedia_lookup = true
  def use_local_lookup = false
  /** show triples with rdf:type */
  def showRDFtype: Boolean
  /** show + Buttons for creating multi-valued */
  def showPlusButtons: Boolean
  /** inline Javascript In Form; overwise app. developer must put it in <head> */
  def inlineJavascriptInForm: Boolean

  /**
   * display Technical Semantic Web Details;
   *  currently whether to display in tooltip URI's of property
   */
  def displayTechnicalSemWebDetails: Boolean

  def useTextQuery: Boolean
  /** considered if useTextQuery */
  def solrIndexing: Boolean

  def css: CSS

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

trait ConfigurationCopy extends Configuration {
  val original: Configuration

  override def showPlusButtons = original.showPlusButtons
  override def inlineJavascriptInForm = original.inlineJavascriptInForm
  override def recordUserActions: Boolean = original.recordUserActions
  override val addRDFS_label_comment = original.addRDFS_label_comment
  override val showRDFtype = original.showRDFtype
  override val css = original.css
  override val displayTechnicalSemWebDetails = original.displayTechnicalSemWebDetails
  override val useTextQuery = original.useTextQuery
  override val solrIndexing = original.solrIndexing

}
