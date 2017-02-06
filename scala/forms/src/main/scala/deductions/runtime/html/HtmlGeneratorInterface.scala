package deductions.runtime.html

import java.net.URLEncoder
import java.security.MessageDigest

import scala.xml.NodeSeq

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.HTTPrequest

/**
 * Pure abstract Interface for HTML Generation from abstractFormSyntax;
 *  maybe TODO remove numerous arguments, to keep mostly request */
trait HtmlGeneratorInterface[NODE, URI <: NODE] {

  def generateHTML(form: FormModule[NODE, URI]#FormSyntax,
                   hrefPrefix: String = "",
                   editable: Boolean = false,
                   actionURI: String = "/save", graphURI: String = "",
                   actionURI2: String = "/save", lang: String = "en",
                   request: HTTPrequest = HTTPrequest()): NodeSeq

  def generateHTMLJustFields(form: FormModule[NODE, URI]#FormSyntax,
                             hrefPrefix: String = "",
                             editable: Boolean = false,
                             graphURI: String = "", lang: String = "en",
                             request: HTTPrequest = HTTPrequest()): NodeSeq
}