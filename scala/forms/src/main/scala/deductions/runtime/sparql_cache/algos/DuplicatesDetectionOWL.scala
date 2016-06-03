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
  val owlFile = args(0)
  val graph = turtleReader.read( new FileReader(owlFile), "") .get
  val duplicates = findDuplicateDataProperties(graph)
  println( s"duplicates size ${duplicates.duplicates.size}")
  val v = duplicates.duplicates.map { dup => dup toString(graph) }
  println( v . mkString("\n") )
  println( s"duplicates size ${duplicates.duplicates.size}")
}


trait DuplicatesDetectionOWL[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  case class Duplicate(d1: Rdf#Node, d2: Rdf#Node) {
    def toString(graph: Rdf#Graph): String = {
      def toString(n: Rdf#Node) =
        d1.toString().replace("http://data.onisep.fr/ontologies/", ":") +
          " \"" + rdfsLabel(d1, graph) + "\"";

      toString(d1) +
      " ~ " +
      toString(d2)
    }
  }
  case class DuplicationAnalysis( duplicates: List[Duplicate] )

  
  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
	  println(s"Triple count ${graph.size}")
    val datatypeProperties = find(graph, ANY, rdf.typ, owl.DatatypeProperty)
    val datatypePropertiesURI = datatypeProperties.map { triple => triple.subject } . toList
    println(s"datatype Properties count ${datatypePropertiesURI.size}")
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2). toList
    println(s"datatype Properties pairs count ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs if (haveSimilarLabels(pair, graph))
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
    if( intersection.size > 0 )
      println(s"""intersection.size >0  $intersection - "$s1" "$s2" """)
    intersection.size > averageSize * 0.5
  }
  
  def rdfsLabel( n: Rdf#Node, graph: Rdf#Graph) =
		  (PointedGraph( n , graph) / rdfs.label).as[String].getOrElse("")
}
