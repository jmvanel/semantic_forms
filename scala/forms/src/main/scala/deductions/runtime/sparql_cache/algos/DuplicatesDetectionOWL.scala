package deductions.runtime.sparql_cache.algos

import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

// deductions.runtime.sparql_cache.algos.DuplicatesDetectionOWLApp
object DuplicatesDetectionOWLApp extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"
// "http://www.w3.org/2001/XMLSchema#" -> "xsd:"
  val owlFile = args(0)
  val graph = turtleReader.read( new FileReader(owlFile), "") .get
  val duplicates = findDuplicateDataProperties(graph)
  ouput( s"duplicates size ${duplicates.duplicates.size}\n")

  val v = duplicates.duplicates.map { dup => dup toString(graph) }
  ouput( v . mkString("\n") )
  ouput( s"duplicates size ${duplicates.duplicates.size}")
}


trait DuplicatesDetectionOWL[Rdf <: RDF]
extends DuplicatesDetectionBase[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
    ouput(s"Triple count ${graph.size}")
    val datatypeProperties = find(graph, ANY, rdf.typ, owl.DatatypeProperty)
    val datatypePropertiesURI = datatypeProperties.map { triple => triple.subject }.toList
    ouput(s"datatype Properties count ${datatypePropertiesURI.size}")
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2).toList
    ouput(s"datatype Properties pairs count n*(n-1)/2 = ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs
      pairList: List[Rdf#Node] = pair.toList
      datatypeProperty1 :: datatypeProperty2 :: rest = pairList if (
    		  nodesAreSimilar(datatypeProperty1, datatypeProperty2, graph) )
      //        _ = log(s"pair $pair")
    } yield Duplicate(datatypeProperty1, datatypeProperty2)

    DuplicationAnalysis(pairs.toList)
  }

}
