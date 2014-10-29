/**
 *
 */

package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDFDSL
import org.w3.banana.OWLPrefix

/**
 * @author j.m. Vanel
 *
 */
object UnfilledFormFactory {
  var instanceURIPrefix = "http://assemblee-virtuelle.org/resource/"
  def makeId : String = {
    val r = instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime()
//    currentId = currentId + 1
    r
  }
}

import UnfilledFormFactory._

class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph,
    preferedLanguage:String="en")
    (implicit ops: RDFOps[Rdf],
    		uriOps: URIOps[Rdf] )
  extends FormSyntaxFactory[Rdf](graph: Rdf#Graph, preferedLanguage) {
  
  /** create Form from a class URI */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    val props = fieldsFromClass(classs, graph)
    createForm(ops.makeUri(makeId), props toSeq)
  }

}