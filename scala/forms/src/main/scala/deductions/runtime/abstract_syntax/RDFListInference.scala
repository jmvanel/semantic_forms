package deductions.runtime.abstract_syntax
import org.w3.banana.RDF
import deductions.runtime.dataset.RDFOPerationsDB
import org.w3.banana.Prefix
import org.w3.banana.OWLPrefix
import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFPrefix
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers

/**
 * populate Fields in form by inferencing from RDF Lists
 */
trait RDFListInference[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI] {

  def makeRDFListEntry(
    label: String, comment: String,
    property: ObjectProperty = nullURI,
    value: Rdf#Node = nullURI,
    alreadyInDatabase: Boolean = true,
    possibleValues: Seq[(Rdf#Node, Rdf#Node)] = Seq(),
    valueLabel: String = "",
    type_ : Rdf#Node = nullURI,
    inverseTriple: Boolean = false,
    subject: Rdf#Node = nullURI,
    mandatory: Boolean = false,
    openChoice: Boolean = true,
    widgetType: WidgetType = Text): Option[RDFListEntry] = {

    val list = getRDFList(value.toString()): List[Rdf#Node]

    list match {
      case l if !l.isEmpty => Some(RDFListEntry(
        label: String, comment: String,
        property: ObjectProperty,
        value: Rdf#Node,
        alreadyInDatabase: Boolean,
        possibleValues: Seq[(Rdf#Node, Rdf#Node)],
        valueLabel: String,
        type_ : Rdf#Node,
        inverseTriple: Boolean,
        subject: Rdf#Node,
        mandatory: Boolean,
        openChoice: Boolean,
        widgetType: WidgetType,
        values = list))
      case _ => None
    }
  }
}