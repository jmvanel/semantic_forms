package deductions.runtime.services

import deductions.runtime.html.CSS

/**
 * All Configuration flags and values for the library;
 * 100% abstract, ie all members are abstract
 */
trait Configuration {

  /** URI Prefix prepended to newly created resource instances */
  def defaultInstanceURIHostPrefix: String
  /** otherwise use defaultInstanceURIHostPrefix */
  def useLocalHostPrefixForURICreation: Boolean

  /** When subject URI's are loaded in batch, and there are duplicates, this lists the Prefixes that will be preferred when merging URI's */
  def preferredURIPrefixes: Seq[String]

  /** get the actual port by server API*/
  def serverPort: String

  def relativeURIforCreatedResourcesByForm: String
  def relativeURIforCreatedResourcesByLDP = relativeURIforCreatedResourcesByForm

  def defaultReadTimeout: Int
  def defaultConnectTimeout: Int
  def httpHeadTimeout: Int

  /** vocabulary for form specifications */
  def formVocabPrefix: String
  def prefixAVontology: String = "prefixAVontology"

  def needLoginForEditing: Boolean
  def needLoginForDisplaying: Boolean

  def recordUserActions: Boolean
  def showDomainlessProperties: Boolean
  def addRDFS_label_comment: Boolean

  def lookup_domain_unionOf: Boolean

  def use_dbpedia_lookup: Boolean
  def use_local_lookup: Boolean
  /** show triples with rdf:type */
  def showRDFtype: Boolean
  /** show + Buttons for creating multi-valued */
  def showPlusButtons: Boolean
  /** show EDIT Buttons for a popup to edit large texts */
  def showEditButtons: Boolean
  /** show all 3 buttons beside the current items in display or edit mode */
  def showExpertButtons: Boolean

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

  def radioForIntervals: Boolean //  = false 
  //  def activateUserInputHistory = false

  /** CORS */
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

  override def defaultInstanceURIHostPrefix = original.defaultInstanceURIHostPrefix
  override def useLocalHostPrefixForURICreation = original.useLocalHostPrefixForURICreation
  override def relativeURIforCreatedResourcesByForm = original.relativeURIforCreatedResourcesByForm
  override def relativeURIforCreatedResourcesByLDP = original.relativeURIforCreatedResourcesByLDP
  override def preferredURIPrefixes = original.preferredURIPrefixes
  override def serverPort = original.serverPort
  override def defaultReadTimeout = original.defaultReadTimeout
  override def defaultConnectTimeout = original.defaultConnectTimeout
  override def httpHeadTimeout = original.httpHeadTimeout

  override def formVocabPrefix = original.formVocabPrefix
  override def prefixAVontology = original.prefixAVontology
  override def needLoginForEditing = original.needLoginForEditing
  override def needLoginForDisplaying = original.needLoginForDisplaying
  override def recordUserActions = original.recordUserActions
  override def showDomainlessProperties = original.showDomainlessProperties
  override def addRDFS_label_comment = original.addRDFS_label_comment
  override def lookup_domain_unionOf = original.lookup_domain_unionOf
  override def use_dbpedia_lookup = original.use_dbpedia_lookup
  override def use_local_lookup = original.use_local_lookup
  override def showRDFtype = original.showRDFtype
  override def showPlusButtons = original.showPlusButtons
  override def showEditButtons = original.showEditButtons
  override def showExpertButtons = original.showExpertButtons
  override def inlineJavascriptInForm = original.inlineJavascriptInForm
  override def displayTechnicalSemWebDetails = original.displayTechnicalSemWebDetails
  override val css = original.css
  override def radioForIntervals = original.radioForIntervals
  override val useTextQuery = original.useTextQuery
  override val solrIndexing = original.solrIndexing
  override def allow_Origin = original.allow_Origin
  override def hrefDisplayPrefix = original.hrefDisplayPrefix
  override def hrefDownloadPrefix = original.hrefDownloadPrefix
  override def hrefEditPrefix = original.hrefEditPrefix
}
