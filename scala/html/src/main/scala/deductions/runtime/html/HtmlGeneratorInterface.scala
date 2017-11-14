package deductions.runtime.html

import deductions.runtime.core.FormModule
import deductions.runtime.utils.RDFPrefixesInterface
import deductions.runtime.core.HTTPrequest

import scala.xml.NodeSeq

/**
 * Pure abstract Interface for HTML Generation from abstract FormSyntax;
 *  maybe TODO remove numerous arguments, to keep mostly request
 *  TODO move to core/ */
trait HtmlGeneratorInterface[NODE, URI <: NODE] extends RDFPrefixesInterface {

  /** generate HTML form given Form Syntax, adding a form header (title, etc)
   *  @param actionURI action URI for top SAVE button
   *         (button not generated if URI = "")
   *  @param actionURI2 action URI for bottom SAVE button
   *         (button not generated if URI = "")
   *  */
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String,
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en",
                   request: HTTPrequest,
                   cssForURI: String = "",
                   cssForProperty: String = ""
                   ): NodeSeq

  /** generate HTML form given Form Syntax, without a form header, just fields */
  def generateHTMLJustFields(form: FormModule[NODE, URI]#FormSyntax,
                             hrefPrefix: String,
                             editable: Boolean = false,
                             graphURI: String = "", lang: String = "en",
                             request: HTTPrequest,
                             cssForURI: String = "",
                             cssForProperty: String = ""
                             ): NodeSeq
}