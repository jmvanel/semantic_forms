package deductions.runtime.sparql_cache.algos

import java.util.StringTokenizer
import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.jena.JenaModule
import org.w3.banana.jena.Jena

// deductions.runtime.sparql_cache.algos.DuplicatesDetectionOWLApp
object DuplicatesDetectionOWLApp extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  val owlFile = args(0)
  val graph = turtleReader.read( new FileReader(owlFile), "") .get
  val duplicates = findDuplicateDataProperties(graph)
  ouput( s"duplicates size ${duplicates.duplicates.size}")
  val v = duplicates.duplicates.map { dup => dup toString(graph) }
  ouput( v . mkString("\n") )
  ouput( s"duplicates size ${duplicates.duplicates.size}")
}


trait DuplicatesDetectionOWL[Rdf <: RDF] {
  val ontologyPrefix: String
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  case class Duplicate(d1: Rdf#Node, d2: Rdf#Node) {
    def toString(graph: Rdf#Graph): String = {
      def toString(n: Rdf#Node) = {
  n.toString().replace(ontologyPrefix, ":") +
    " \"" + rdfsLabel(n, graph) + "\""
};

      toString(d1) +
      " ~ " +
      toString(d2)
    }
  }
  case class DuplicationAnalysis( duplicates: List[Duplicate] )

  
  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
	  ouput(s"Triple count ${graph.size}")
    val datatypeProperties = find(graph, ANY, rdf.typ, owl.DatatypeProperty)
    val datatypePropertiesURI = datatypeProperties.map { triple => triple.subject } . toList
    ouput(s"datatype Properties count ${datatypePropertiesURI.size}")
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2). toList
    ouput(s"datatype Properties pairs count ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs if (haveSimilarLabels(pair, graph))
    	  _ = log(s"pair $pair")
    } yield {
      pair.toList match {
        case (datatypeProperty1 :: datatypeProperty2 :: rest) => Duplicate(datatypeProperty1, datatypeProperty2)
        case _ => Duplicate(???, ???) // will not happen :)
      }
    }
    DuplicationAnalysis( pairs.toList )
  }

  def haveSimilarLabels(pair: Set[Rdf#Node], graph: Rdf#Graph): Boolean = {
    pair.toList match {
      case (datatypeProperty1 :: datatypeProperty2 :: rest) =>
        val ranges1 = find(graph, datatypeProperty1, rdfs.range, ANY)
        val ranges2: Iterator[Rdf#Triple] = find(graph, datatypeProperty2, rdfs.range, ANY)
        val rangesOverlap = !ranges1.toSet.intersect(ranges2.toSet).isEmpty
        // TODO use rangesOverlap ; how ?
        
        val label1 = rdfsLabel( datatypeProperty1, graph)
        val label2 = rdfsLabel( datatypeProperty2, graph)
        areSimilar(label1, label2)
      case _ => false
    }
  }

  def areSimilar(s1: String, s2: String): Boolean = {
    val words1 = s1.split("""\s+""").toSet
    val words2 = s2.split("""\s+""").toSet
    val intersection = words1 intersect (words2)
    val averageSize = ( words1.size +  words2.size ) * 0.5
    val output = intersection.size > averageSize * 0.5
    if( output ) // intersection.size > 0 )
      log(s"""\tintersection.size >0  $intersection - "$s1" "$s2" """)
    output
  }
  
  def rdfsLabel( n: Rdf#Node, graph: Rdf#Graph) =
		  (PointedGraph( n , graph) / rdfs.label).as[String].getOrElse("")

  def log(mess: String) = if(detailedLog) println(mess)
  def ouput(mess: String) = println(mess)
}
