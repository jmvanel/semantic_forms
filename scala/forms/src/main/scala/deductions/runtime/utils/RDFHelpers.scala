package deductions.runtime.utils

import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDF
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._

/**
 * use with :
 *  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
 */
abstract class RDFHelpers[Rdf <: RDF](implicit ops: RDFOps[Rdf]) {
  val graph: Rdf#Graph
  val rdf = RDFPrefix[Rdf]

  /** recursively iterate on the Rdf#Node through rdf:first and rdf:rest */
  def rdfListToSeq(listOp: Option[Rdf#Node], result: Seq[Rdf#Node] = Seq()): Seq[Rdf#Node] = {
    listOp match {
      case None => result
      case Some(list) =>
        list match {
          case rdf.nil => result
          case _ =>
            val first = ops.getObjects(graph, list, rdf.first)
            val rest = ops.getObjects(graph, list, rdf.rest)
            result ++ first ++ rdfListToSeq(rest.headOption, result)
        }
    }
  }

  def nodeSeqToURISeq(s: Iterable[Rdf#Node]): Seq[Rdf#URI] = {
    val r = s.collect {
      case uri if (isURI(uri)) => ops.makeUri(uri.toString)
    }
    val seq = r.to
    seq
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

  /** Query for objects in triples, given subject & predicate */
  def objectsQuery(subject: Rdf#Node, predicate: Rdf#URI): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf](subject, graph)
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }
}