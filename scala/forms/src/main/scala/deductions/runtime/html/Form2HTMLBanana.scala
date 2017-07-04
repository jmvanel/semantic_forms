package deductions.runtime.html

import deductions.runtime.utils.Configuration
import org.w3.banana.{RDF, RDFOps}

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

object Form2HTMLObject {
  def makeDefaultForm2HTML[Rdf <: RDF](configuration: Configuration)(implicit ops: RDFOps[Rdf]):
  Form2HTMLBanana[Rdf] // Note: implements HtmlGeneratorInterface[Rdf#Node, Rdf#URI]
  = {
    lazy val ops1 = ops
    new Form2HTMLBanana[Rdf] {
      lazy val nullURI = ops.URI("")
      lazy val ops: org.w3.banana.RDFOps[Rdf] = ops1
      lazy val config: Configuration = configuration

      // example of overriding a function implementing the form:

//      override def makeBackLinkButton(uri: String, title: String = "") = {
//        val tit = if (title == "") s" Reverse links for &lt;$uri&gt;" else title
//        <button type="button" class="btn btn-info" readonly="yes" title={ tit } data-value={ s"$uri" } onclick={ s"backlinks( '$uri' )" } id={ s"BACK-$uri" }>
//          bla
//        </button>
//  }
    }
  }
}