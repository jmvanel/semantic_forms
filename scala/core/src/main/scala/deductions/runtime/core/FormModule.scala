/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.core

import scala.collection.mutable
import scalaz._
import Scalaz._

object FormModule {
  val formDefaults = FormDefaults()
}

/** Default values for the whole Form or for an `Entry` */
case class FormDefaults(
                         var defaultCardinality: Cardinality = zeroOrMore,
                         /* displaying rdf:type fields is configurable for editing, and displayed unconditionally for non editing */
                         val displayRdfType: Boolean = true) {
  def multivalue: Boolean = defaultCardinality == zeroOrMore ||
    defaultCardinality == oneOrMore
}

/** data structures for user forms around RDF triples
 *  TODO , LITERAL <: NODE */
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

                         /** several types */
                         val classs: Seq[NODE] = Seq(),
                         formGroup: URI = nullURI,
                         val defaults: FormDefaults = FormModule.formDefaults,

                         /** properties Groups are groups within the form that can be shown by tabs
                           *  (currently properties of super-classes)
                           *  TODO there should be only one of propertiesGroups and propertiesGroupMap,
                           * propertiesGroups is actually used in Form2HTML,
                           * propertiesGroupMap is intermediary data
                           * (unfinished refactoring of RawDataForForms */
                         propertiesGroups: collection.Seq[FormSyntax] = collection.Seq[FormSyntax](),
                         propertiesGroupMap: collection.Map[NODE, FormSyntax] =
                           collection.Map[NODE, FormSyntax](),

                         val title: String = "",
                         val thumbnail: Option[NODE] = None,
                         val formURI: Option[NODE] = None,
                         val formLabel: String = "",
                         val editable: Boolean = false,
                         val reversePropertiesList: Seq[NODE] = Seq()
                       ) {

    def setSubject(subject: NODE, editable: Boolean): FormSyntax = {

      val propertiesGroupsWithSubject = propertiesGroupMap.map {
        case (node, formSyntax) => (node,
          formSyntax.setSubject(subject, editable))
      }

      FormSyntax(subject,fields,classs,formGroup,defaults, propertiesGroupMap = propertiesGroupsWithSubject)
    }

    /** comes from a refactor, maybe remove */
    def propertiesList: Seq[NODE] = {
      fields . map ( _.property )
    }

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

    def nonEmptyFields(): Seq[Entry] = {
      fields . filter { f =>
        f.value.toString()  =/=  "" &&
        f.value.toString()  =/=  "\"\""
      }
//      println(s"==== nonEmptyFields: $ret")
    }

    def typeEntries() = {
      nonEmptyFields().filter {
        // TODO why not rdf:type ?
        entry => entry.property .toString() . endsWith("type")
      }
    }

    def types() = {
      typeEntries().map {
        entry => entry.value
      }
    }
  }


  val nullFormSyntax = FormSyntax(nullURI, Seq() )

  type DatatypeProperty = URI
  type ObjectProperty = NODE // URI
  case class TripleLocal(val s: NODE, val p: URI, val o: NODE)

  val nullURI: URI


  def makeURI(n: NODE): URI = nullURI
  def stringToAbstractURI(uri: String): URI
  def toPlainString(n: NODE): String

  val NullResourceEntry = new ResourceEntry("", "", nullURI, ResourceValidator(Set()))
  val NullLiteralEntry = new LiteralEntry("", "", nullURI)

  /** an entry (for an RDF triple) in a form */
  abstract class Entry	{
    val label: String
    val comment: String
    val property: NODE
    /** unused yet :( */
    val mandatory: Boolean// = false
    /** several types */
//    val type_ : NODE
    val type_ : Seq[NODE]
    val value: NODE // = nullURI
    val subjectLabel: String
    val widgetType: WidgetType // = URIWidget
    /** openChoice allows user in form to choose a value not in suggested values
      * true <=> user has possibility to type any (valid) data */
    val openChoice: Boolean// = true
    val possibleValues: Seq[(NODE, NODE)] // = Seq()
    val defaults: FormDefaults = FormModule.formDefaults
    /** for multi-subject forms */
    val subject: NODE // TODO should be a type generalizing URI and BLANK !
    val cardinality: Cardinality// = zeroOrMore
    val htmlName: String

    /** user URI */
    val metadata: String = ""
    /** timeStamp of last modification */
    val timeMetadata: Long = -1

    /** filled, not not used*/
    private val triples: mutable.Buffer[TripleLocal] = mutable.ListBuffer[TripleLocal]()

    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry
    override def toString(): String = {
      s"""Entry $subject <$property> value $value. ${getClass.getSimpleName} label "$label", "$comment" "$widgetType", openChoice: $openChoice, metadata '$metadata'"""
    }
    def addTriple(s: NODE, p: URI, o: NODE) = {
      val t = TripleLocal(s, p, o)
      triples :+ t
    }

    def asResource(): ResourceEntry = {
      this match {
        case r:ResourceEntry => r
        case _ => NullResourceEntry
      }
    }

    /** string version of value; actually used in form generation */
    def valueLabel: String = ""

    def isClass: Boolean = false

    /** clone this - TODO remove copy - paste !!!! */
    def copyEntry(
      cardinality: Cardinality = Entry.this.cardinality,
      widgetType: WidgetType = Entry.this.widgetType,
      fromProperty: NODE = Entry.this.property,
      fromMetadata: String = Entry.this.metadata,
      fromTimeMetadata: Long = Entry.this.timeMetadata,
      label: String = Entry.this.label,
      value: NODE = Entry.this.value
      ): Entry = {
      this match {
        case r: ResourceEntry => r.copy(
          cardinality = cardinality,
          widgetType = widgetType,
          property = fromProperty,
          metadata = fromMetadata,
          timeMetadata = fromTimeMetadata,
          label = label,
          value = value)
        case e: LiteralEntry => e.copy(
          cardinality = cardinality,
          widgetType = widgetType,
          property = fromProperty,
          metadata = fromMetadata,
          timeMetadata = fromTimeMetadata,
          label = label,
          value = value)
        case e: BlankNodeEntry => e.copy(
          cardinality = cardinality,
          widgetType = widgetType,
          property = fromProperty,
          value = value)
        case e: RDFListEntry => e.copy(
          cardinality = cardinality,
          widgetType = widgetType,
          property = fromProperty,
          value = value)
        case e: Entry =>
          System.err.println(s"copyEntry: copying plain entry !!!! $e")
          e

      }
    }
  }

  def makeEntries(propertiesList: Seq[NODE]): Seq[Entry] =
    propertiesList.map {
      prop => makeEntry(prop)
  }
    
  /** clone this
   * TODO bad practice of pasting all fields, use copy */
  private def makeEntry(fromProperty: NODE = nullURI, fromMetadata: String = ""): Entry = {
    new Entry {
      override val property = fromProperty
      override val metadata = fromMetadata

      val cardinality: deductions.runtime.core.Cardinality = zeroOrMore
      val comment: String = ""
      val htmlName: String = ""
      val label: String = ""
      val mandatory: Boolean = false
      val openChoice: Boolean = true
      val possibleValues: Seq[(NODE, NODE)] = Seq()
      def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry = ???
      val subject: NODE = nullURI
      val subjectLabel: String = ""
//      val type_ : NODE = nullURI
      val type_ : Seq[NODE] = Seq()

      val value: NODE = nullURI
      val widgetType: WidgetType = URIWidget
    }
  }

  /** @param possibleValues a couple of an RDF node id and the label to display, see trait RangeInference */
  case class ResourceEntry(
                            label: String="",
                            override val comment: String="",
                            override val property: ObjectProperty = nullURI,
                            val validator: ResourceValidator = ResourceValidator(Set()),
                            override val value: NODE = nullURI, val alreadyInDatabase: Boolean = true,
                            possibleValues: Seq[(NODE, NODE)] = Seq(),
                            override val valueLabel: String = "",
                            override val type_ : Seq[NODE]= Seq(),
                            val inverseTriple: Boolean= false,
                            override val subject: NODE = nullURI,
                            override val subjectLabel: String = "",
                            override val mandatory: Boolean = false,
                            openChoice: Boolean = true,
                            widgetType: WidgetType = URIWidget,
                            cardinality: Cardinality = zeroOrMore,
                            /** is the value itself an Image? */
                            val isImage: Boolean = false,
                            /** possible thumbnail Image for the value */
                            val thumbnail: Option[NODE] = None,
                            override val htmlName: String = "",

                            override val metadata: String = "",
                            override val timeMetadata: Long = -1,
                            override val isClass: Boolean = false
                          )
    extends Entry {
    override def toString(): String = {
      "RESOURCE " + super.toString +
        s"""; <$value>, valueLabel "$valueLabel", image <$thumbnail> possibleValues count=${possibleValues.size} """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      //      val ret = new ResourceEntry(label, comment,
      //        property, validator,
      //        value, alreadyInDatabase,
      //        newPossibleValues, valueLabel, type_ )
      this.copy(possibleValues = newPossibleValues)
      //      ret.openChoice = this.openChoice
      //      ret.widgetType = this.widgetType
      //      ret
    }

    def this(e: Entry, validator: ResourceValidator,
             alreadyInDatabase: Boolean,
             valueLabel: String) = this(
      e.label: String,
      e.comment: String,
      e.property, validator,
      makeURI(e.value),
      alreadyInDatabase,
      e.possibleValues,
      valueLabel,
//      makeURI
      (e.type_),
      false,
      e.subject,
      e.subjectLabel,
      e.mandatory,
      e.openChoice,
      e.widgetType,
      e.cardinality,
      false,
      None, // thumbnail
      e.htmlName,
      e.metadata,
      e.timeMetadata
    )
  }


  case class BlankNodeEntry(
                             label: String="", comment: String="",
                             property: ObjectProperty = nullURI,
                             validator: ResourceValidator = ResourceValidator(Set()),
                             value: NODE,
//                             type_ : NODE = nullURI,
                             type_ : Seq[NODE] = Seq(),

                             possibleValues: Seq[(NODE, NODE)] = Seq(),
                             override val valueLabel: String = "",
                             subject: NODE = nullURI,
                             override val subjectLabel: String = "",
                             val mandatory: Boolean = false,
                             openChoice: Boolean = true,
                             widgetType: WidgetType = URIWidget,
                             cardinality: Cardinality = zeroOrMore,
                             val isImage: Boolean = false,
                             val thumbnail: Option[NODE] = None,
                             val htmlName: String = "" )

    extends Entry {
    override def toString(): String = {
      "BN: " + super.toString + s", <$value> , possibleValues count:${possibleValues.size}"
    }
    def getId: String = value.toString
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      //      val ret = new BlankNodeEntry(label, comment,
      //        property, validator, value, type_, newPossibleValues)
      //      ret.openChoice = this.openChoice
      //      ret.widgetType = this.widgetType
      //      ret
      this.copy(possibleValues = newPossibleValues)
      //        label, comment, property, validator, value, type_, possibleValues, valueLabel, subject, subjectLabel, mandatory, openChoice, widgetType, cardinality, isImage, thumbnail, htmlName)
    }
  }


  case class LiteralEntry(
                           label: String="", comment: String="",
                           property: NODE /* DatatypeProperty */ = nullURI,
                           validator: DatatypeValidator = DatatypeValidator(Set()),
                           value: NODE = nullURI, // String = "",
                           val lang: String = "",
//                           type_ : NODE = nullURI,
                           type_ : Seq[NODE] = Seq(),
                           possibleValues: Seq[(NODE, NODE)] = Seq(),
                           subject: NODE = nullURI,
                           override val subjectLabel: String = "",
                           val mandatory: Boolean = false,
                           openChoice: Boolean = true,
                           widgetType: WidgetType = Textarea,
                           cardinality: Cardinality = zeroOrMore,
                           /** string version of value; actually used in form generation */
                           override val valueLabel: String = "",
                           val htmlName: String = "",

                           override val metadata: String = "",
                           override val timeMetadata: Long = -1)

    extends Entry {

    override def toString(): String = {
      "LiteralEntry " + super.toString // + s""" value := '$value' """
    }
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = {
      //      val ret = new LiteralEntry(label, comment,
      //        property, validator,
      //        value, lang, type_,
      //        newPossibleValues)
      //      ret.openChoice = this.openChoice
      //      ret.widgetType = this.widgetType
      //      ret
      this.copy(possibleValues = newPossibleValues)
    }

    override def asResource(): ResourceEntry = {
      new ResourceEntry(this,
        validator = null,
        alreadyInDatabase = true,
        valueLabel = this.value.toString()
      )
    }
  }

  case class RDFListEntry(
                           label: String, comment: String,
                           property: ObjectProperty = nullURI,
                           value: NODE = nullURI,
                           alreadyInDatabase: Boolean = true,
                           possibleValues: Seq[(NODE, NODE)] = Seq(),
                           override val valueLabel: String = "",
//                           type_ : NODE = nullURI,
                           type_ : Seq[NODE] = Seq(),
                           inverseTriple: Boolean = false,
                           subject: NODE = nullURI,
                           override val subjectLabel: String = "",
                           val mandatory: Boolean = false,
                           openChoice: Boolean = true,
                           widgetType: WidgetType = ListWidget,
                           val values: FormSyntax,
                           cardinality: Cardinality = exactlyOne,
                           val htmlName: String = ""
                         ) extends Entry {
    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]) = this
  }


  case class ResourceValidator(typ: Set[NODE])
  case class DatatypeValidator(typ: Set[NODE])

}

sealed class WidgetType
abstract class Text extends WidgetType { override def toString() = "Text WidgetType" }
object Textarea extends Text
object ShortString extends Text { override def toString() = "Short Text WidgetType" }
object Checkbox extends WidgetType

abstract class Choice extends WidgetType
/** Can be Radio Button or checkboxes for multiple choices, depending on Entry.openChoice */
object Buttons extends Choice
object Slider extends Choice
object PulldownMenu extends Choice

/** */
object URIWidget extends WidgetType { override def toString() = "URI WidgetType" }
object ListWidget extends WidgetType { override def toString() = "List WidgetType" }
object DBPediaLookup extends WidgetType { override def toString() = "DBPediaLookup WidgetType" }
object SPARQLvirtuosoLookup extends WidgetType { override def toString() = "SPARQLvirtuosoLookup WidgetType" }
object UpLoad extends WidgetType


sealed class Cardinality
object zeroOrMore extends Cardinality { override def toString() = "0:*" }
object oneOrMore extends Cardinality { override def toString() = "1:*" }
object zeroOrOne extends Cardinality { override def toString() = "0:1" }
object exactlyOne extends Cardinality { override def toString() = "1:1" }