package deductions.runtime.utils

import org.w3.banana.RDF
import deductions.runtime.core.FormModule
import org.w3.banana.RDFOps

trait FormModuleBanana[Rdf <: RDF]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFPrefixes[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  override def toPlainString(n: Rdf#Node): String =
    foldNode(n)(fromUri(_),
      bn => {
        val s = fromBNode(bn)
        if (s.startsWith("_:")) s
        else "_:" + s
      },
      fromLiteral(_)._1)

  override def stringToAbstractURI(uri: String): Rdf#URI =
    URI(expandOrUnchanged(uri))
}