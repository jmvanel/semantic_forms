package deductions.runtime.html

import org.w3.banana.RDF
import org.w3.banana.RDFOps

/**
 * @author jmv
 */
trait Form2HTMLBanana[Rdf <: RDF] extends Form2HTML[Rdf#Node, Rdf#URI] {
  implicit val ops: RDFOps[Rdf]
  import ops._
  override def toPlainString(n: Rdf#Node): String =
    foldNode(n)(fromUri(_), fromBNode(_), fromLiteral(_)._1)
}