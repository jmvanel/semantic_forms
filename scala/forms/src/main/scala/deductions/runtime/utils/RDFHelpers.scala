package deductions.runtime.utils

import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDF

abstract class RDFHelpers[Rdf <: RDF] // extends RDFOpsModule
(implicit ops: RDFOps[Rdf]) {
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

  def nodeSeqToURISeq(s: Seq[Rdf#Node]): Seq[Rdf#URI] = {
    s.collect {
      case uri if (isURI(uri)) => ops.makeUri(uri.toString)
    }
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

}