/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import org.w3.banana._
import org.w3.banana.diesel._
import org.w3.banana.syntax._
import scala.collection.mutable

trait FormModule[NODE, URI <: NODE] {
  /**
   * abstract_syntax for a semantic form , called FA (Abstract Form) :
   *  - generated from a list of URI's for properties, and a triple store
   *  - used in conjunction with HTML5 forms and Banana-RDF
   *  - could be used with N3Form(Swing) in EulerGUI,
   */
  case class FormSyntax[NODE, URI <: NODE](
      val subject: NODE,
      val fields: Seq[Entry],
      classs: URI = nullURI,
      formGroup: URI = nullURI) {
    override def toString(): String = {
      s"""FormSyntax:
        subject: $subject
        classs: $classs
        ${fields.mkString("\n")}
      """
    }
  }

  type DatatypeProperty = URI
  type ObjectProperty = URI
  case class Triple(val s: NODE, val p: URI, val o: NODE)

  val nullURI: URI
  /**
   * openChoice allows user in form to choose a value not in suggested values
   *  TODO somehow factor value: Any ?
   */
  sealed abstract class Entry(
      val label: String, val comment: String,
      val property: URI = nullURI,
      val mandatory: Boolean = false,
      val type_ : URI = nullURI,
      val value: Any = "",
      var widgetType: WidgetType = Text,
      var openChoice: Boolean = true,
      var possibleValues: Seq[(NODE, NODE)] = Seq()) {
    private val triples: mutable.Buffer[Triple] = mutable.ListBuffer[Triple]()
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry
    override def toString(): String = {
      s""" "$label", "$comment" $widgetType """
    }
    def addTriple(s: NODE, p: URI, o: NODE) = {
      val t = Triple(s, p, o)
      triples :+ t
    }
  }

  /** @param possibleValues a couple of an RDF node id and the label to display, see trait RangeInference */
  class ResourceEntry(label: String, comment: String,
    property: ObjectProperty = nullURI, validator: ResourceValidator,
    value: URI = nullURI, val alreadyInDatabase: Boolean = true,
    possibleValues: Seq[(NODE, NODE)] = Seq(),
    val valueLabel: String = "",
    type_ : URI = nullURI)
      extends Entry(label, comment, property, type_ = type_, value = value, possibleValues = possibleValues) {
    override def toString(): String = {
      super.toString + s""" : <$value>, "$valueLabel" possibleValues count:${possibleValues.size} """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      new ResourceEntry(label, comment,
        property, validator,
        value, alreadyInDatabase,
        newPossibleValues, valueLabel, type_)
    }
  }
  class BlankNodeEntry(label: String, comment: String,
      property: ObjectProperty = nullURI, validator: ResourceValidator,
      value: NODE, type_ : URI = nullURI) extends Entry(label, comment, property, type_ = type_, value = value) {
    override def toString(): String = {
      super.toString + ", " + value
    }
    def getId: String = value.toString
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      new BlankNodeEntry(label, comment,
        property, validator, value)
    }
  }
  class LiteralEntry(l: String, c: String,
      property: DatatypeProperty = nullURI, validator: DatatypeValidator,
      value: String = "",
      val lang: String = "",
      type_ : URI = nullURI,
      possibleValues: Seq[(NODE, NODE)] = Seq()) extends Entry(l, c, property, type_ = type_, value = value, possibleValues = possibleValues) {
    override def toString(): String = {
      super.toString + s""" := "$value" """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      new LiteralEntry(label, comment,
        property, validator,
        value, lang, type_,
        newPossibleValues)
    }
  }

  case class ResourceValidator(typ: Set[NODE]) // URI])
  case class DatatypeValidator(typ: Set[NODE]) // URI])

}

sealed class WidgetType
object Text extends WidgetType { override def toString() = "Text WidgetType" }
object Textarea extends WidgetType
object Checkbox extends WidgetType

abstract class Choice extends WidgetType
/** Can be Radio Button or checkboxes for multiple choices, depending on Entry.openChoice */
object Buttons extends Choice
object Slider extends Choice
object PulldownMenu extends Choice

object Collection extends WidgetType
object DBPediaLookup extends WidgetType { override def toString() = "DBPediaLookup WidgetType" }

