package deductions.runtime.sparql_cache.algos

import java.util.StringTokenizer

import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix

trait DuplicatesDetectionOWL[Rdf <: RDF] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  def findDuplicateDataProperties(graph: Rdf#Graph) = {
    val datatypeProperties = find(graph, ANY, rdf.typ, owl.DatatypeProperty)
    for {
      datatypeProperty_triple1 <- datatypeProperties
      datatypeProperty_triple2 <- datatypeProperties
      datatypeProperty1 = datatypeProperty_triple1.subject
      datatypeProperty2 = datatypeProperty_triple2.subject
      if (datatypeProperty1 != datatypeProperty2)
    } {
      val ranges1 = find(graph, datatypeProperty1, rdfs.range, ANY )
      val ranges2: Iterator[Rdf#Triple] = find(graph, datatypeProperty2, rdfs.range, ANY )
      val rangesOverlap = ! ranges1.toSet.intersect(ranges2.toSet) . isEmpty
      
      val label1 = ( PointedGraph( datatypeProperty1, graph ) / rdfs.label ) . as[String] . getOrElse("")
      val label2 = ( PointedGraph( datatypeProperty2, graph ) / rdfs.label ) . as[String] . getOrElse("")
      
      areSimilar( label1, label2 )
      // TODO ???????????? @return the list of pairs of similar property URI's
    }
  }

  def areSimilar(s1: String, s2: String): Boolean = {
    val st1 = new StringTokenizer(s1)
    val st2 = new StringTokenizer(s2)
    ???
  }
}
