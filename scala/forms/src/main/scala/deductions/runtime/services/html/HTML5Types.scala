package deductions.runtime.services.html

import deductions.runtime.html.HTML5Types
import org.w3.banana.{RDF, XSDPrefix}
import org.w3.banana.RDFOps

import scalaz._
import Scalaz._

//object HTML5TypesJena extends JenaModule with HTML5TypesTrait[Jena]

/**
 * HTML5 input type correspondence to XML Schema (XSD) datatypes
 *  TODO : intervals
 */
trait HTML5TypesTrait[Rdf <: RDF] extends HTML5Types {
  implicit val ops: RDFOps[Rdf]
  import ops._

  private lazy val xsd = XSDPrefix[Rdf]

  def xsd2html5TnputType(xsdDatatype: String): String =
    xsd2html5.getOrElse(URI(xsdDatatype), "text")
  def xsdNode2html5TnputType(xsdDatatype: Rdf#Node): String =
    xsd2html5.getOrElse(xsdDatatype, "text")

    /** TODO set the right delta for each XSD number type */
    def xsd2html5Step(xsdDatatype: String): String =
      if(xsd2html5TnputType(xsdDatatype) === "number")
        "any"
      else
        null

  /** see http://www.w3.org/TR/html-markup/input.html */
  lazy val xsd2html5 = Map[Rdf#Node, String](
    xsd.string -> "text",
    xsd.integer -> "number",
    xsd.int -> "number",
    xsd.float -> "number",
    xsd.decimal -> "number",
    xsd.long -> "number",
    xsd.short -> "number",
    xsd.double -> "number",
    xsd.nonNegativeInteger -> "number",
    xsd.nonPositiveInteger -> "number",
    xsd.negativeInteger -> "number",
    xsd.positiveInteger -> "number",

    xsd.dateTime -> "datetime-local",
    xsd.dateTimeStamp -> "datetime-local",
    xsd("date") -> "datetime-local",

    xsd.anyURI -> "url",
    xsd.boolean -> "radio"
  /* Symfony stuff:
   * ⓘ input type=text
ⓘ input type=password
ⓘ input type=checkbox
ⓘ input type=radio
ⓘ input type=button
ⓘ input type=submit
ⓘ input type=reset
ⓘ input type=file
ⓘ input type=hidden
ⓘ input type=image

ⓘ input type=datetime-local NEW
ⓘ input type=date NEW
ⓘ input type=month NEW
ⓘ input type=time NEW
ⓘ input type=week NEW
ⓘ input type=number NEW
ⓘ input type=range NEW
ⓘ input type=email NEW
ⓘ input type=search NEW
ⓘ input type=tel NEW
ⓘ input type=color NEW
     */
  )
}