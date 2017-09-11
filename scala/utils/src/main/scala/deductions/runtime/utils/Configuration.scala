package deductions.runtime.utils

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

//  def prefixAVontology: String = "prefixAVontology"

  def needLoginForEditing: Boolean
  def needLoginForDisplaying: Boolean

  def recordUserActions: Boolean
  def showDomainlessProperties: Boolean
  def addRDFS_label_comment: Boolean
  def groupFields: Boolean

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
  /**
   * display Technical Semantic Web Details;
   *  currently whether to display in tooltip URI's of property
   */
  def displayTechnicalSemWebDetails: Boolean
  /** download Possible Values in edit forms (otherwise, just rely on /lookup service) */
  def downloadPossibleValues: Boolean
  def useTextQuery: Boolean
  /** considered if useTextQuery */
  def solrIndexing: Boolean

  def css: CSSClasses

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
