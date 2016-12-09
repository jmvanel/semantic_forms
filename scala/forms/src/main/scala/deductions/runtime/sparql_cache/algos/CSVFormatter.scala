package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral

trait CSVFormatter[Rdf <: RDF, DATASET]
    extends DuplicatesDetectionBase[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with InstanceLabelsInference2[Rdf] {

  private lazy val owl = OWLPrefix[Rdf]

  def formatCSVLinesForNode(node: Rdf#Node, classToReportURI: Rdf#URI= owl.Class,
      propertiesToReport: List[Rdf#URI]= List() )(implicit graph: Rdf#Graph): String = {

    val rdfs_domain = rdfsDomain(node, graph)
    val domainLabel = rdfsLabel(rdfs_domain, graph)
    val superClassesLabel = rdfsSuperClasses(rdfs_domain, graph).
      map { superC => rdfsLabel(superC, graph) }.
      mkString(", ")
    val rdfs_range = rdfsRange(node, graph)
    val rangeLabel = rdfsLabel(rdfs_range.headOption, graph)
    val contextLabelProperty = domainLabel + (if (!superClassesLabel.isEmpty) " --> " + superClassesLabel else "")
    val contextLabel = classToReportURI match {
      case owl.ObjectProperty   => contextLabelProperty
      case owl.DatatypeProperty => contextLabelProperty
      case owl.Class            => rdfsPropertiesAndRangesFromClass(node, graph)
    }
    val digestFromClass = "\t" +
      (if (classToReportURI == owl.Class)
        makeDigestFromClass(node, graph)
      else "")

    val otherProperties = propertiesToReport.map {
      p => printPropertyValueNoDefault(node, graph, p)
    }.mkString("\t")

    val label = rdfsLabel(node, graph)
    val id = abbreviateURI(node)
    val fullLine = s"\t'${label}'\t" + id + "\t" + contextLabel + "\t" +
      rangeLabel + digestFromClass + "\t" + otherProperties

    fullLine + detailLines(node)
  }

  /** detail Lines in between full Lines */
  def detailLines(n: Rdf#Node)(implicit graph: Rdf#Graph): String = {
    val propertiesAndRangesFromClass = rdfsPropertiesAndRangesFromClassListDetails(n, graph)

    val classLabel = instanceLabel(n, graph, "fr")
    val firstColumns = s"\t'$classLabel' (P)\t\t\t"
    if (propertiesAndRangesFromClass.isEmpty)
      ""
    else
      propertiesAndRangesFromClass.mkString("\n" + firstColumns, "\n" + firstColumns, "")
  }
}
