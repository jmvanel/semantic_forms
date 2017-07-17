package deductions.runtime.html

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.{HTTPrequest, RDFPrefixesInterface}

import scala.xml.NodeSeq

/**
 * Pure abstract Interface for HTML Generation from abstract FormSyntax;
 *  maybe TODO remove numerous arguments, to keep mostly request */
trait HtmlGeneratorInterface[NODE, URI <: NODE] extends RDFPrefixesInterface {

  /** generate HTML form given Form Syntax, adding a form header (title, etc) */
  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String,
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en",
                   request: HTTPrequest
                   ): NodeSeq

  /** generate HTML form given Form Syntax, without a form header, just fields */
  def generateHTMLJustFields(form: FormModule[NODE, URI]#FormSyntax,
                             hrefPrefix: String,
                             editable: Boolean = false,
                             graphURI: String = "", lang: String = "en",
                             request: HTTPrequest
                             //= HTTPrequest()
                             ): NodeSeq
}