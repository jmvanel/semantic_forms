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
class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph)(implicit ops: RDFOps[Rdf],
  uriOps: URIOps[Rdf],
  rdfDSL: RDFDSL[Rdf])
  extends FormSyntaxFactory[Rdf](graph: Rdf#Graph) {
  val owl = OWLPrefix[Rdf]
  /** create Form from an instance (subject) URI */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    val props = fieldsFromClass(classs, graph)
    createForm(nullURI, props toSeq)
  }

  private def fieldsFromClass(classs: Rdf#URI, graph: Rdf#Graph): Set[Rdf#URI] = {
    def domainFromClass(classs: Rdf#Node) = {
      val relevantPredicates = rdfDSL.getSubjects(graph, rdfs.domain, classs).toSet
      extractURIs(relevantPredicates) toSet
    }
    
    val result = scala.collection.mutable.Set[Rdf#URI]()
    processSuperClasses(classs)
    
    /** recursively process super-classes until reaching owl:Thing */
    def processSuperClasses(classs: Rdf#URI) {
      result ++= domainFromClass(classs)
      if (classs != owl.Thing ) {
        val superClasses = rdfDSL.getObjects(graph, classs, rdfs.subClassOf)
        superClasses foreach (sc => result ++= domainFromClass(sc))
      }
    }
    result.toSet
  }
}