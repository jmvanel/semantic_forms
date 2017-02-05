package deductions.runtime.html

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.Configuration

/**
 * @author jmv
 */
trait Form2HTMLBanana[Rdf <: RDF]
    extends Form2HTML[Rdf#Node, Rdf#URI]
    with HtmlGeneratorInterface[Rdf#Node, Rdf#URI]
    with HTML5TypesTrait[Rdf] {

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
}

//object Form2HTMLObject {
//  def makeForm2HTML()
//  // (implicit ops: RDFOps[ImplementationSettings.Rdf], config: Configuration)
//  = {
//    val ops1 = ops
//    val config1 = config
//    new Form2HTMLBanana[ImplementationSettings.Rdf] {
//      val nullURI = ops.URI("")
//      val ops: org.w3.banana.RDFOps[deductions.runtime.jena.ImplementationSettings.Rdf] = ops1
//      val config: Configuration = config1
//    }
//  }
//}