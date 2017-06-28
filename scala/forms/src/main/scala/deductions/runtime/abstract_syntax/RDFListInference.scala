package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * populate Fields in form by inferencing from RDF Lists
 */
trait RDFListInference[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
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
    subjectLabel: String = "",
    mandatory: Boolean = false,
    openChoice: Boolean = true,
    widgetType: WidgetType = Text): Option[RDFListEntry] = {

    val graph = allNamedGraph
//    val nodesList = getRDFList(makeTurtleTerm(value)): List[Rdf#Node]
    val nodesList = nodeSeqToURISeq(rdfListToSeq(Some(value))(allNamedGraph))

    println(s"makeRDFListEntry list $nodesList")
    val entriesList: Seq[Entry] = nodesList . map {
      node => ops.foldNode(node)(
          uri => ResourceEntry(value=uri, valueLabel=makeInstanceLabel(node, graph, "en")),
          bn => BlankNodeEntry(value=bn, valueLabel=makeInstanceLabel(node, graph, "en")),
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
        subjectLabel: String,
        mandatory: Boolean,
        openChoice: Boolean,
        widgetType: WidgetType,
        values = list))
      case _ => None
    }
  }
}