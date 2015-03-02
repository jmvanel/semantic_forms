/**
 *
 */

package deductions.runtime.abstract_syntax

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.URIOps
import UnfilledFormFactory.defaultInstanceURIPrefix
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps

/**
 * @author j.m. Vanel
 *
 */
object UnfilledFormFactory {
  var defaultInstanceURIPrefix = "http://assemblee-virtuelle.org/resource/"
  /** make a unique Id with given prefix, currentTimeMillis() and nanoTime() */
  def makeId(instanceURIPrefix: String): String = {
    instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
  }
}

/** Factory for an Unfilled Form */
class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph,
  preferedLanguage: String = "en",
  instanceURIPrefix: String = defaultInstanceURIPrefix)(implicit ops: RDFOps[Rdf],
    uriOps: URIOps[Rdf],
    rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET],
    sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph],
    sparqlOps: SparqlOps[Rdf])
    extends FormSyntaxFactory[Rdf](graph: Rdf#Graph, preferedLanguage) //    with SparqlGraphModule
    {

  //  val gr = graph
  //  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
  //  val formConfiguration = new FormConfigurationFactory[Rdf](graph)
  import formConfiguration._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph in this class
   */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    val formConfig = lookPropertieslistFormInConfiguration(classs)
    if (formConfig.isEmpty) {
      val props = fieldsFromClass(classs, graph)
      createFormDetailed(ops.makeUri(makeId), props toSeq, classs)
    } else
      createFormDetailed(ops.makeUri(makeId), formConfig.toSeq, classs)
  }

  def makeId: String = {
    //      instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
    UnfilledFormFactory.makeId(instanceURIPrefix)
  }
}