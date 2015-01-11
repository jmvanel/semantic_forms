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
      classs: URI = nullURI) {
    override def toString(): String = {
      s"""FormSyntax:
        subject: $subject
      """ + fields.mkString("\n")
    }
  }

  type DatatypeProperty = URI
  type ObjectProperty = URI
  case class Triple(val s: NODE, val p: URI, val o: NODE)

  val nullURI: URI
  /** TODO somehow factor out value: Any ? */
  sealed abstract class Entry(
      val label: String, val comment: String,
      val property: URI = nullURI,
      val mandatory: Boolean = false,
      val type_ : URI = nullURI //      widgetType: WidgetType = Text
      ) {
    private val triples: mutable.Buffer[Triple] = mutable.ListBuffer[Triple]()
    override def toString(): String = {
      s""" "$label", "$comment" """
    }
    def addTriple(s: NODE, p: URI, o: NODE) = {
      val t = Triple(s, p, o)
      triples :+ t
    }
  }

  class ResourceEntry(label: String, comment: String,
    property: ObjectProperty = nullURI, validator: ResourceValidator,
    val value: URI = nullURI, val alreadyInDatabase: Boolean = true,
    val possibleValues: Seq[(URI, String)] = Seq(),
    val valueLabel: String = "",
    type_ : URI = nullURI)
      extends Entry(label, comment, property, type_ = type_) {
    override def toString(): String = {
      super.toString + s""" : <$value>, "$valueLabel" possibleValues count:${possibleValues.size} """
    }
    def setPossibleValues(newPossibleValues: Seq[(URI, String)]) = {
      new ResourceEntry(label, comment,
        property, validator,
        value, alreadyInDatabase,
        newPossibleValues, valueLabel, type_)
    }
  }
  class BlankNodeEntry(label: String, comment: String,
      property: ObjectProperty = nullURI, validator: ResourceValidator,
      val value: NODE, type_ : URI = nullURI) extends Entry(label, comment, property, type_ = type_) {
    override def toString(): String = {
      super.toString + ", " + value
    }
    def getId: String = value.toString
  }
  class LiteralEntry(l: String, c: String,
      property: DatatypeProperty = nullURI, validator: DatatypeValidator,
      val value: String = "",
      type_ : URI = nullURI) extends Entry(l, c, property, type_ = type_) {
    override def toString(): String = {
      super.toString + s""" := "$value" """
    }
  }

  case class ResourceValidator(typ: Set[URI])
  case class DatatypeValidator(typ: Set[URI])

  sealed class WidgetType
  object Text extends WidgetType
  object Textarea extends WidgetType
  object Checkbox extends WidgetType
  object Choice extends WidgetType
  object Collection extends WidgetType
  object DBPediaLookup extends WidgetType
}
