package deductions.runtime.utils

import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDF
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._
import org.w3.banana.syntax._
import scala.util._

/**
 * use with :
 *  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
 */
abstract class RDFHelpers[Rdf <: RDF](implicit ops: RDFOps[Rdf]) {
  val graph: Rdf#Graph
  val rdf = RDFPrefix[Rdf]
  import ops._

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

  /** from given Set of Rdf#Node , extract rdf#URI */
  def nodeSeqToURISeq(s: Iterable[Rdf#Node]): Seq[Rdf#URI] = {
    val r = s.collect {
      case uri if (isURI(uri)) => ops.makeUri(uri.toString)
    }
    val seq = r.to
    seq
  }

  /** from given Set of Rdf#Node , extract rdf#URI */
  def nodeSeqToURISet(s: Iterable[Rdf#Node]): Set[Rdf#URI] = {
    nodeSeqToURISeq(s).toSet
  }

  /**
   * from given Set of Rdf#Node , extract rdf#URI
   *  TODO : check that it's the same as nodeSeqToURISet
   */
  private def extractURIs(nodes: Set[Rdf#Node]): Set[Rdf#URI] = {
    nodes.map {
      node =>
        ops.foldNode(node)(
          identity, identity, x => None
        )
    }
      .filter(_ != None)
      .map { node => node.asInstanceOf[Rdf#URI] }
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

  /** Query for objects in triples, given subject & predicate */
  def objectsQuery(subject: Rdf#Node, predicate: Rdf#URI): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf](subject, graph)
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }

  def objectsQueries[T <: Rdf#Node](subjects: Set[T], predicate: Rdf#URI): Set[Rdf#Node] = {
    val values = for (
      subject <- subjects;
      values <- objectsQuery(subject.asInstanceOf[Rdf#URI], predicate)
    ) yield values
    values
  }

  def getStringOrElse(n: Rdf#Node, default: String): String = {
    ops.foldNode(n)(_ => default, _ => default, l => {
      val v = ops.fromLiteral(l)
      v._1
    })
  }

  def getNodeOrElse(n: Rdf#Node, default: String): Rdf#Node = {
    val d = ops.Literal(default)
    ops.foldNode(n)(_ => d, _ => d, l => l)
  }

  def printGraph(graph: Rdf#Graph) {
    val iterable = ops.getTriples(graph)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }
}