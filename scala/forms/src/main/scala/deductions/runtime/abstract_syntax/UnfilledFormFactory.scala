/**
 *
 */

package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDFDSL
import org.w3.banana.OWLPrefix
import UnfilledFormFactory._
import org.w3.banana.RDFPrefix
import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDFStore
import deductions.runtime.jena.RDFStoreObject
import scala.util.Try
import org.apache.log4j.Logger

/**
 * @author j.m. Vanel
 *
 */
object UnfilledFormFactory {
  var defaultInstanceURIPrefix = "http://assemblee-virtuelle.org/resource/"
}

/** Factory for an Unfilled Form */
class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph,
  preferedLanguage: String = "en",
  instanceURIPrefix: String = defaultInstanceURIPrefix)(implicit ops: RDFOps[Rdf],
    uriOps: URIOps[Rdf],
    rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET])
    extends FormSyntaxFactory[Rdf](graph: Rdf#Graph, preferedLanguage) {

//  val gr = graph
//  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
  val formConfiguration = new FormConfigurationFactory[Rdf](graph)
  import formConfiguration._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph in this class
   */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    val formConfig = lookPropertieslistFormInConfiguration(classs)
    if (formConfig.isEmpty) {
      val props = fieldsFromClass(classs, graph)
      createForm(ops.makeUri(makeId), props toSeq, classs)
    } else
      createForm(ops.makeUri(makeId), formConfig.toSeq, classs)
  }

//  /** lookup for form:showProperties (ordered list of fields) in Form Configuration within RDF graph in this class */
//  private def lookFormConfiguration(classs: Rdf#URI): Seq[Rdf#URI] = {
//    val rdf = RDFPrefix[Rdf]
//    val forms = ops.getSubjects(graph, formPrefix("classDomain"), classs)
//    val b = new StringBuilder; Logger.getRootLogger().debug("forms " + forms.addString(b, "; "))
//    val formNodeOption = forms.flatMap {
//      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
//    }.headOption
//    Logger.getRootLogger().debug( "formNodeOption " + formNodeOption )
//    formNodeOption match {
//      case None => Seq()
//      case Some(f) =>
//        // val props = ops.getObjects(graph, f, form("showProperties"))
//        val props = oQuery(f, formPrefix("showProperties"))
//        for (p <- props) { println("showProperties " + p) }
//        val p = props.headOption
//        rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(p))
//    }
//  }

  def makeId: String = {
    val r = instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
    r
  }
}