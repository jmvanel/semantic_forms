package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import deductions.runtime.core.FormModule

/** unused - a step in form generation */
trait FormSyntaxTransform[Rdf <: RDF] extends FormModule[Rdf#Node, Rdf#URI] {

  def process(formSyntax: FormSyntax)(implicit graph: Rdf#Graph,
                                      lang: String = ""): FormSyntax

  /** unused yet */
  def process2(formSyntax: FormSyntax)(implicit modelsGraph: Rdf#Graph,
                                       dataGraph: Rdf#Graph,
                                       lang: String = ""): FormSyntax
}
