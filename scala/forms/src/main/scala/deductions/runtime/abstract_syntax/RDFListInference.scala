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
    with FormModule[Rdf#Node, Rdf#URI]
    with PreferredLanguageLiteral[Rdf]
    with InstanceLabelsInference2[Rdf] {

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

    val graph = allNamedGraph
    val nodesList = getRDFList(makeTurtleTerm(value)): List[Rdf#Node]
    println(s"makeRDFListEntry list $nodesList")
    val entriesList: Seq[Entry] = nodesList . map {
      node => ops.foldNode(node)(
          uri => ResourceEntry(value=uri, valueLabel=instanceLabel(node, graph, "en")),
          bn => BlankNodeEntry(value=bn, valueLabel=instanceLabel(node, graph, "en")),
          lit => LiteralEntry(value=lit)
      )
    }
    val list = FormSyntax(nullURI, entriesList)

    nodesList match {
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