/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import scala.collection.mutable

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

trait FormModule[NODE, URI <: NODE] {

  /**
   * abstract_syntax for a semantic form , called AF (Abstract Form) :
   *  - generated from a list of URI's for properties, and a triple store
   *  - used in conjunction with HTML5 forms and Banana-RDF
   *  - could be used with N3Form(Swing) in EulerGUI,
   *  TODO: put language as a field?
   */
  def makeEntries(propertiesList: Seq[NODE]): Seq[Entry] =
    propertiesList.map {
      prop => makeEntry(prop)
    }
  case class FormSyntax(
      val subject: NODE,
      var fields: Seq[Entry],

      /** TODO remove entriesList (replaced by fields)
       *  cf commit
       *  "REFACTORING : moving each field on formSyntax + changing all rawDatForForm by formSyntax" */
      val entriesList: Seq[FormModule[NODE, URI]#Entry] = Seq(),

      classs: NODE = nullURI,
      formGroup: URI = nullURI,
      val defaults: FormDefaults = FormModule.formDefaults,
      // TODO maybe : propertiesGroups could be a list of FormSyntax
//      propertiesGroups: collection.Map[NODE, Seq[Entry]] = collection.Map[NODE, Seq[Entry]](),
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

      FormSyntax(subject,fields,entriesList,classs,formGroup,defaults, propertiesGroupMap = propertiesGroupsWithSubject)
    }

    /** TODO <<<<<<<<<< comes from a refactor */
    def propertiesList: Seq[NODE] = {
      entriesList . map ( _.property )
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
  }

  val nullFormSyntax = FormSyntax(nullURI, Seq() )

  type DatatypeProperty = URI
  type ObjectProperty = NODE // URI
  case class Triple(val s: NODE, val p: URI, val o: NODE)

  val nullURI: URI
  def makeURI(n: NODE): URI = nullURI

  val NullResourceEntry = new ResourceEntry("", "", nullURI, ResourceValidator(Set()))


  /** an entry (for an RDF triple) in a form */
   abstract class Entry	{
	  val label: String 
	  val comment: String 
	  val property: NODE
	  /** unused yet :( */
	  val mandatory: Boolean// = false
	  /** TODO : several types */
	  val type_ : NODE// = nullURI
//	  val type_ : Seq[NODE] // TODO <<<<<<<<<<<<<<<<
	  val value: NODE // = nullURI
    val subjectLabel: String 
	  val widgetType: WidgetType // = URIWidget
	  /** openChoice allows user in form to choose a value not in suggested values
	   * true <=> user has possibility to type any (valid) data */
	  val openChoice: Boolean// = true
	  val possibleValues: Seq[(NODE, NODE)] // = Seq()
	  val defaults: FormDefaults = FormModule.formDefaults
	  /** for multi-subject forms */
		val subject: NODE// = nullURI
    val cardinality: Cardinality// = zeroOrMore
    val htmlName: String

    /** user URI */
    val metadata: String = ""
    /** timeStamp of last modification */
    val timeMetadata: Long = -1

    /** filled, not not used*/
    private val triples: mutable.Buffer[Triple] = mutable.ListBuffer[Triple]()

    def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry
    override def toString(): String = {
      s"""${getClass.getSimpleName} label "$label", "$comment" "$widgetType", openChoice: $openChoice"""
    }
    def addTriple(s: NODE, p: URI, o: NODE) = {
      val t = Triple(s, p, o)
      triples :+ t
    }

    def asResource(): ResourceEntry = {
      this match {
        case r:ResourceEntry => r
        case _ => NullResourceEntry
      }
    }

    def valueLabel: String = ""

    /** clone this
     *  PENDING bad practice of pasting all fields */

    def makeEntry(
        fromProperty: NODE = Entry.this.property,
        fromMetadata: String = Entry.this.metadata,
        fromTimeMetadata: Long = Entry.this.timeMetadata ): Entry = {

      this match {
        case r: ResourceEntry => r.copy(
          property = fromProperty,
          metadata = fromMetadata,
          timeMetadata = fromTimeMetadata)
        case e: LiteralEntry => e.copy(
          property = fromProperty,
          metadata = fromMetadata,
          timeMetadata = fromTimeMetadata)
        case e: BlankNodeEntry => e.copy()
        case e: RDFListEntry   => e.copy()
      }
//      new Entry {
//        override val property = fromProperty
//        override val metadata = fromMetadata
//
//        val cardinality: deductions.runtime.abstract_syntax.Cardinality = Entry.this.cardinality
//        val comment: String = Entry.this.comment
//        val htmlName: String = Entry.this.htmlName
//        val label: String = Entry.this.label
//        val mandatory: Boolean = Entry.this.mandatory
//        val openChoice: Boolean = Entry.this.openChoice
//        val possibleValues: Seq[(NODE, NODE)] = Entry.this.possibleValues
//        def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry = ???
//        val subject: NODE = Entry.this.subject
//        val subjectLabel: String = Entry.this.subjectLabel
//        val type_ : NODE = Entry.this.type_
//        val value: NODE = Entry.this.value
//        val widgetType: deductions.runtime.abstract_syntax.WidgetType = Entry.this.widgetType
//      }
    }
  }

  def makeEntry(fromProperty: NODE = nullURI, fromMetadata: String = ""): Entry = {
    new Entry {
      override val property = fromProperty
      override val metadata = fromMetadata

      val cardinality: deductions.runtime.abstract_syntax.Cardinality = zeroOrMore
      val comment: String = ""
      val htmlName: String = ""
      val label: String = ""
      val mandatory: Boolean = false
      val openChoice: Boolean = true
      val possibleValues: Seq[(NODE, NODE)] = Seq()
      def setPossibleValues(newPossibleValues: Seq[(NODE, NODE)]): Entry = ???
      val subject: NODE = nullURI
      val subjectLabel: String = ""
      val type_ : NODE = nullURI
      val value: NODE = nullURI
      val widgetType: deductions.runtime.abstract_syntax.WidgetType = URIWidget
    }
  }

  /** @param possibleValues a couple of an RDF node id and the label to display, see trait RangeInference */
  case class ResourceEntry(
		override val label: String="",
		override val comment: String="",
    override val property: ObjectProperty = nullURI,
    val validator: ResourceValidator = ResourceValidator(Set()),
    override val value: NODE = nullURI, val alreadyInDatabase: Boolean = true,
    possibleValues: Seq[(NODE, NODE)] = Seq(),
    override val valueLabel: String = "",
    override val type_ : NODE = nullURI,
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
    override val timeMetadata: Long = -1
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
      "BN: " + super.toString + s", $value , possibleValues count:${possibleValues.size}"
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
    type_ : NODE = nullURI,
    possibleValues: Seq[(NODE, NODE)] = Seq(),
    subject: NODE = nullURI,
    override val subjectLabel: String = "",
    val mandatory: Boolean = false,
    openChoice: Boolean = true,
    widgetType: WidgetType = Text,
    cardinality: Cardinality = zeroOrMore,
    override val valueLabel: String = "",
    val htmlName: String = "",

    override val metadata: String = "",
    override val timeMetadata: Long = -1)

      extends Entry {

    override def toString(): String = {
      super.toString + s""" := '$value' """
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
      type_ : NODE = nullURI,
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
object Text extends WidgetType { override def toString() = "Text WidgetType" }
object Textarea extends WidgetType
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
object UpLoad extends WidgetType


sealed class Cardinality
object zeroOrMore extends Cardinality { override def toString() = "0:*" }
object oneOrMore extends Cardinality { override def toString() = "1:*" }
object zeroOrOne extends Cardinality { override def toString() = "0:1" }
object exactlyOne extends Cardinality { override def toString() = "1:1" }

