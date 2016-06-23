package deductions.runtime.sparql_cache.algos

import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import scala.collection.immutable.ListMap

/** Duplicates Detection for OWL; output: CSV, Grouped By labels of Datatype properties */
object DuplicatesDetectionOWLGroupBy extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
  val addEmptyLineBetweenLabelGroups = true
  
  val owlFile = args(0)
  val graph = turtleReader.read(new FileReader(owlFile), "").get
  val datatypePropertiesURI = findDataProperties(graph)
  val datatypePropertiesgroupedByRdfsLabel0 = datatypePropertiesURI.
    groupBy { n => rdfsLabel(n, graph) }
  val datatypePropertiesgroupedByRdfsLabel = ListMap(datatypePropertiesgroupedByRdfsLabel0.toSeq.
    sortBy(_._1): _*)
  val report = formatCSV()
  // formatIndentedText()
  output(s"$report")

  /** format report as CSV */
  def formatCSV(): String = {
    /** format Label Group as CSV */
    def formatCSVLines(labelAndList: (String, List[Rdf#Node])) = {
      val list = labelAndList._2
      val columns = for (n <- list) yield {
        val rdfs_domain = rdfsDomain(n, graph)
        val domainLabel = rdfsLabel(rdfs_domain, graph)
        val superClassesLabel = rdfsSuperClasses(rdfs_domain, graph).
          map { superC => rdfsLabel(superC, graph) }.
          mkString(", ")
        val contextLabel = domainLabel + (if (!superClassesLabel.isEmpty) " --> " + superClassesLabel else "")
        s"'${labelAndList._1}'\t" + abbreviateURI(n) + "\t" + contextLabel
      }
      columns.mkString("\n") + (
        if (addEmptyLineBetweenLabelGroups)
          "\n"
        else "")
    }
    // TODO I18N
    output("Libellé_propriété	Id_propriété	Contexte	Action")
    datatypePropertiesgroupedByRdfsLabel.map {
      labelAndList => formatCSVLines(labelAndList)
    }.mkString("\n")
  }

  def formatIndentedText() = {
    datatypePropertiesgroupedByRdfsLabel.map {
      labelAndList =>
        s"'${labelAndList._1}'\n" +
          (labelAndList._2).map { n => abbreviateURI(n) }.sorted.mkString("\t", "\n\t", "")
    }.mkString("\n")
  }
}

/**
 * This App oututs too much : count n*(n-1)/2 ;
 *  rather use DuplicatesDetectionOWLGroupBy
 */
object DuplicatesDetectionOWLApp extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
  val owlFile = args(0)
  val graph = turtleReader.read(new FileReader(owlFile), "").get
  val duplicates = findDuplicateDataProperties(graph)
  output(s"duplicates size ${duplicates.duplicates.size}\n")

  val v = duplicates.duplicates.map { dup => dup toString (graph) }
  output(v.mkString("\n"))
  output(s"duplicates size ${duplicates.duplicates.size}")
}

trait DuplicatesDetectionOWL[Rdf <: RDF]
    extends DuplicatesDetectionBase[Rdf] {
  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  implicit val ops: RDFOps[Rdf]
  import ops._

  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
    val datatypePropertiesURI = findDataProperties(graph)
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2).toList
    output(s"datatype Properties pairs count n*(n-1)/2 = ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs
      pairList: List[Rdf#Node] = pair.toList
      datatypeProperty1 :: datatypeProperty2 :: rest = pairList if (
        nodesAreSimilar(datatypeProperty1, datatypeProperty2, graph))
      //        _ = log(s"pair $pair")
    } yield Duplicate(datatypeProperty1, datatypeProperty2)

    DuplicationAnalysis(pairs.toList)
  }

}
