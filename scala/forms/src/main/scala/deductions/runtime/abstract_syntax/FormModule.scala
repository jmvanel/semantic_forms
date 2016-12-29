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

object FormModule {
  val formDefaults = FormDefaults()
}

/** Default values for the whole Form or for an `Entry` */
case class FormDefaults(
    var defaultCardinality: Cardinality = zeroOrMore,
    /** displaying rdf:type fields is configurable for editing, and displayed unconditionally for non editing */
    val displayRdfType: Boolean = true) {
  def multivalue: Boolean = defaultCardinality == zeroOrMore ||
    defaultCardinality == oneOrMore
}

trait FormModule[NODE, URI <: NODE] {

  /**
   * abstract_syntax for a semantic form , called AF (Abstract Form) :
   *  - generated from a list of URI's for properties, and a triple store
   *  - used in conjunction with HTML5 forms and Banana-RDF
   *  - could be used with N3Form(Swing) in EulerGUI,
   *  TODO: put language as a field?
   */
  case class FormSyntax(
      val subject: NODE,
      var fields: Seq[Entry],
      classs: NODE = nullURI,
      formGroup: URI = nullURI,
      val defaults: FormDefaults = FormModule.formDefaults,
      // TODO maybe : propertiesGroups could be a list of FormSyntax
//      propertiesGroups: collection.Map[NODE, Seq[Entry]] = collection.Map[NODE, Seq[Entry]](),
      propertiesGroups: collection.Seq[FormSyntax] = collection.Seq[FormSyntax](),
      val title: String = ""
      ) {
    
    /** Map from property to possible Values  */
	  val possibleValuesMap = scala.collection.mutable.Map[ NODE, Seq[(NODE, NODE)]]()
//    def getPossibleValues( f: Entry) = possibleValuesMap.getOrElse( f.property, Seq() )
    
	  override def toString(): String = {
      s"""FormSyntax:
        subject: $subject
        classs: $classs
        ${fields.mkString("\n")}
      """
    }
  }

  type DatatypeProperty = URI
  type ObjectProperty = NODE // URI
  case class Triple(val s: NODE, val p: URI, val o: NODE)

  val nullURI: URI
  def makeURI(n: NODE): URI = nullURI

  /**
   * openChoice allows user in form to choose a value not in suggested values
   *  TODO somehow factor value: Any ?
   */
   abstract class Entry	{
	  val label: String
	  val comment: String
	  val property: NODE
	  /** unused yet :( */
	  val mandatory: Boolean
	  /** TODO : several types */
	  val type_ : NODE
//	  val type_ : Seq[NODE] // TODO <<<<<<<<<<<<<<<<
	  val value: NODE
	  var widgetType: WidgetType
	  /** true <=> user has possibility to type any (valid) data */
	  var openChoice: Boolean
	  var possibleValues: Seq[(NODE, NODE)]
	  val defaults: FormDefaults = FormModule.formDefaults
	  /** for multi-subject forms */
		val subject: NODE
      
    private val triples: mutable.Buffer[Triple] = mutable.ListBuffer[Triple]()
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry
    override def toString(): String = {
      s"""${getClass.getSimpleName} "$label", "$comment" $widgetType, openChoice: $openChoice"""
    }
    def addTriple(s: NODE, p: URI, o: NODE) = {
      val t = Triple(s, p, o)
      triples :+ t
    }

    def asResource(): Entry = {
      this
    }

    def valueLabel: String = ""
  }


  /** @param possibleValues a couple of an RDF node id and the label to display, see trait RangeInference */
  case class ResourceEntry(
		label: String="", comment: String="",
    property: ObjectProperty = nullURI,
    validator: ResourceValidator = ResourceValidator(Set()),
    value: NODE = nullURI, val alreadyInDatabase: Boolean = true,
    var possibleValues: Seq[(NODE, NODE)] = Seq(),
    override val valueLabel: String = "",
    type_ : NODE = nullURI,
    inverseTriple: Boolean= false,
    subject: NODE = nullURI,
    val mandatory: Boolean = false,
    var openChoice: Boolean = true,
    var widgetType: WidgetType = Text
    )
      extends Entry {
    override def toString(): String = {
      "RES " + super.toString +
      s""" : <$value>, valueLabel "$valueLabel" possibleValues count:${possibleValues.size} """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      val ret = new ResourceEntry(label, comment,
        property, validator,
        value, alreadyInDatabase,
        newPossibleValues, valueLabel, type_)
      ret.openChoice = this.openChoice
      ret.widgetType = this.widgetType
      ret
    }

    def this(e: Entry, validator: ResourceValidator,
      alreadyInDatabase: Boolean,
      valueLabel: String) = this(
      e.label: String, e.comment: String,
      e.property, validator,
      makeURI(e.value),
      alreadyInDatabase,
      e.possibleValues,
      valueLabel
      ,makeURI(e.type_)
      )
  }


  case class BlankNodeEntry(
    label: String="", comment: String="",
    property: ObjectProperty = nullURI,
    validator: ResourceValidator = ResourceValidator(Set()),
    value: NODE, type_ : NODE = nullURI,
    var possibleValues: Seq[(NODE, NODE)] = Seq(),
    override val valueLabel: String = "",
    subject: NODE = nullURI,
    val mandatory: Boolean = false,
    var openChoice: Boolean = true,
    var widgetType: WidgetType = Text )
      extends Entry {
    override def toString(): String = {
      "BN: " + super.toString + s", $value , possibleValues count:${possibleValues.size}"
    }
    def getId: String = value.toString
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      val ret = new BlankNodeEntry(label, comment,
        property, validator, value, type_, newPossibleValues)
      ret.openChoice = this.openChoice
      ret.widgetType = this.widgetType
      ret
    }
  }


  case class LiteralEntry(
    label: String="", comment: String="",
    property: NODE /* DatatypeProperty */ = nullURI,
    validator: DatatypeValidator = DatatypeValidator(Set()),
    value: NODE = nullURI, // String = "",
    val lang: String = "",
    type_ : NODE = nullURI,
    var possibleValues: Seq[(NODE, NODE)] = Seq(),
    subject: NODE = nullURI,
    val mandatory: Boolean = false,
    var openChoice: Boolean = true,
    var widgetType: WidgetType = Text)

      extends Entry {

    override def toString(): String = {
      super.toString + s""" := "$value" """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      val ret = new LiteralEntry(label, comment,
        property, validator,
        value, lang, type_,
        newPossibleValues)
      ret.openChoice = this.openChoice
      ret.widgetType = this.widgetType
      ret
    }

    override def asResource(): Entry = {
      new ResourceEntry(this,
        validator = null,
        alreadyInDatabase = true,
        valueLabel = this.value.toString()
      )
    }
    override def valueLabel: String = value.toString()
  }

  case class RDFListEntry(
      label: String, comment: String,
      property: ObjectProperty = nullURI,
      value: NODE = nullURI,
      val alreadyInDatabase: Boolean = true,
      var possibleValues: Seq[(NODE, NODE)] = Seq(),
      override val valueLabel: String = "",
      type_ : NODE = nullURI,
      inverseTriple: Boolean = false,
      subject: NODE = nullURI,
      val mandatory: Boolean = false,
      var openChoice: Boolean = true,
      var widgetType: WidgetType = Text,
      val values: FormSyntax
      ) extends Entry {
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = this
  }


  case class ResourceValidator(typ: Set[NODE])
  case class DatatypeValidator(typ: Set[NODE])

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
object UpLoad extends WidgetType

sealed class Cardinality
object zeroOrMore extends Cardinality { override def toString() = "0 Or More" }
object oneOrMore extends Cardinality { override def toString() = "1 Or More" }
object zeroOrOne extends Cardinality { override def toString() = "0 Or 1" }
object exactlyOne extends Cardinality { override def toString() = "exactly 1" }

