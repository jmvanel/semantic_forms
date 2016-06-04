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
import deductions.runtime.html.HTML5Types
import deductions.runtime.html.HTML5TypesTrait

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
extends HTML5TypesTrait[Rdf] {
  val ontologyPrefix: String
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  case class Duplicate(d1: Rdf#Node, d2: Rdf#Node) {
    /** cf http://tools.ietf.org/html/rfc4180 */
    def toString(graph: Rdf#Graph): String = {
      def toString(n: Rdf#Node) = {
        n.toString().replace(ontologyPrefix, ":") +
          "; \"" + rdfsLabel(n, graph) + "\""
      };

      val r1 = rdfsRangeToString(d1, graph)
      val r2 = rdfsRangeToString(d2, graph)
      if (r1 != r2)
        toString(d1) + "; rdfs:range " + r1 +
          "; " +
          toString(d2) + "; rdfs:range " + r2
      else
        toString(d1) + ";" +
          "; " +
          toString(d2) + ";"
    }
  }
  case class DuplicationAnalysis( duplicates: List[Duplicate] )

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

  def nodesAreSimilar(n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
	  haveSimilarLabels(n1, n2, graph) &&
	  haveSameRanges(n1, n2, graph)
  }

  def haveSameRanges(n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
    val ranges1 = rdfsRange(n1, graph)
    val ranges2 = rdfsRange(n2, graph)
    //    println(s"ranges1 $ranges1 ranges2 $ranges2")
    val rangesOverlap = !(ranges1.toSet.intersect(ranges2.toSet).isEmpty)
    rangesOverlap
  }
  
  def haveSimilarLabels( n1: Rdf#Node, n2: Rdf#Node, graph: Rdf#Graph): Boolean = {
        val label1 = rdfsLabel(n1, graph)
        val label2 = rdfsLabel(n2, graph)
        stringsAreSimilar(label1, label2)
  }

  def stringsAreSimilar(s1: String, s2: String): Boolean = {
    if( s1 == "" || s2 == "" ) return false

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

  def rdfsRange(n: Rdf#Node, graph: Rdf#Graph) =
    find(graph, n, rdfs.range, ANY).
      map { triple => triple.objectt }.toList

  def rdfsRangeToString(n: Rdf#Node, graph: Rdf#Graph): String = {
    rdfsRange(n, graph) match {
      case range :: rest =>
        xsdNode2html5TnputType(range)
      case _ => n.toString()
    }
  }
      
  def log(mess: String) = if(detailedLog) println(mess)
  def ouput(mess: String) = println(mess)
}
