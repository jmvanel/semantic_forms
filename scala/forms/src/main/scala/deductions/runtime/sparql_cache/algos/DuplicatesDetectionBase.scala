package deductions.runtime.sparql_cache.algos

import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix

import deductions.runtime.html.HTML5TypesTrait



trait DuplicatesDetectionBase[Rdf <: RDF]
extends HTML5TypesTrait[Rdf] {
  val ontologyPrefix: String
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdf = RDFPrefix[Rdf]
  val rdfs = RDFSPrefix[Rdf]
  val owl = OWLPrefix[Rdf]

  
  /** @return the list property URI's */
  def findDataProperties(graph: Rdf#Graph): List[Rdf#Node] = {
    output(s"Triple count ${graph.size}")
    val datatypeProperties = find(graph, ANY, rdf.typ, owl.DatatypeProperty)
    val datatypePropertiesURI = datatypeProperties.map { triple => triple.subject }.toList
    output(s"datatype Properties count ${datatypePropertiesURI.size}\n")
    datatypePropertiesURI
  }

  case class Duplicate(d1: Rdf#Node, d2: Rdf#Node) {
    /** cf http://tools.ietf.org/html/rfc4180 */
    def toString(graph: Rdf#Graph): String = {
      def toString(n: Rdf#Node) = {
        abbreviateURI(n) +
          "; " + rdfsLabel(n, graph)
      };

      val r1 = rdfsRangeToString(d1, graph)
      val r2 = rdfsRangeToString(d2, graph)
      if (r1 != r2)
        toString(d1) + "; rdfs:range " + r1 +
          "; " +
          toString(d2) + "; rdfs:range " + r2
      else
        toString(d1) + ";" + ( if(r1 != "text" ) r1 else "" ) +
          "; " +
          toString(d2) + ";"
    }
  }
  case class DuplicationAnalysis( duplicates: List[Duplicate] )
  
  def abbreviateURI(n: Rdf#Node) = n.toString().replace(ontologyPrefix, ":")
  
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
  def output(mess: String) = println(mess)
}
