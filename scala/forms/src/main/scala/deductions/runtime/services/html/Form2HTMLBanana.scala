package deductions.runtime.services.html

import deductions.runtime.html.HtmlGeneratorInterface
import deductions.runtime.utils.{Configuration, RDFPrefixesInterface}
import org.w3.banana.{RDF, RDFOps}
import deductions.runtime.html._
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.FormModuleBanana

/**
 * @author jmv
 */
trait Form2HTMLBanana[Rdf <: RDF]
    extends FormModuleBanana[Rdf]
    with Form2HTML[Rdf#Node, Rdf#URI]
    with HtmlGeneratorInterface[Rdf#Node, Rdf#URI]
    with HTML5TypesTrait[Rdf] {}


object Form2HTMLObject {
  def makeDefaultForm2HTML[Rdf <: RDF](configuration: Configuration)(implicit ops: RDFOps[Rdf]):
  Form2HTMLBanana[Rdf] // Note: implements HtmlGeneratorInterface[Rdf#Node, Rdf#URI]
  = {
    lazy val ops1 = ops
    new Form2HTMLBanana[Rdf] with RDFPrefixes[Rdf] {
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