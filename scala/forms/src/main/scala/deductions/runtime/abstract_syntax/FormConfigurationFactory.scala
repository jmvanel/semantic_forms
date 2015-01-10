package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDFStore
import deductions.runtime.jena.RDFStoreObject
import org.w3.banana.OWLPrefix
import UnfilledFormFactory._
import org.w3.banana.RDFPrefix
import deductions.runtime.utils.RDFHelpers
import org.w3.banana.RDFStore
import scala.util.Try
import org.apache.log4j.Logger
import org.w3.banana.Prefix

/**
 * Factory for an Unfilled Form
 *  TODO extract code for form:showProperties (ordered list of fields);
 */
class FormConfigurationFactory[Rdf <: RDF](graph: Rdf#Graph)(implicit ops: RDFOps[Rdf],
                                                             uriOps: URIOps[Rdf],
                                                             rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET]) {

  val formPrefix: Prefix[Rdf] = Prefix("form", FormSyntaxFactory.formVocabPrefix)
  val gr = graph
  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
  import rdfh._
  
  /** lookup for form:showProperties (ordered list of fields) in Form Configuration within RDF graph in this class
   *  usable for unfilled and filled Forms */
  def lookPropertieslistFormInConfiguration(classs: Rdf#URI): Seq[Rdf#URI] = {
    val rdf = RDFPrefix[Rdf]
    val forms = ops.getSubjects(graph, formPrefix("classDomain"), classs)
    val b = new StringBuilder; Logger.getRootLogger().debug("forms " + forms.addString(b, "; "))
    val formNodeOption = forms.flatMap {
      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
    }.headOption
    Logger.getRootLogger().debug("formNodeOption " + formNodeOption)
    formNodeOption match {
      case None => Seq()
      case Some(f) =>
        // val props = ops.getObjects(graph, f, form("showProperties"))
        val props = objectsQuery(f, formPrefix("showProperties"))
        for (p <- props) { println("showProperties " + p) }
        val p = props.headOption
        rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(p))
    }
  }
}