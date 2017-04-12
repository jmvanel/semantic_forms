package deductions.runtime.abstract_syntax

import scala.collection.mutable
import scala.language.existentials
import scala.language.postfixOps

import org.w3.banana.RDF
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.XSDPrefix

import deductions.runtime.services.Configuration
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.Timer
import org.w3.banana.PointedGraph

trait UserTraceability[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI] {

  def addUserInfoOnTriples(
    formSyntax: FormSyntax)(
      implicit graph: Rdf#Graph,
      lang: String = "en") : FormSyntax = {
	  formSyntax
  }
}