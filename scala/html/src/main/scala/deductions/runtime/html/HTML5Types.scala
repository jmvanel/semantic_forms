package deductions.runtime.html

import deductions.runtime.utils.RDFPrefixesInterface

trait HTML5Types extends RDFPrefixesInterface {
  def xsd2html5TnputType(xsdDatatype: String): String
  def xsd2html5Step(xsdDatatype: String): String
}