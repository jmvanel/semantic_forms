package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix
import org.w3.banana.FOAFPrefix
import deductions.runtime.utils.RDFHelpers
import scala.collection._
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFOpsModule

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait InstanceLabelsInference2[Rdf <: RDF] extends RDFOpsModule {

  import ops._
  val foaf = FOAFPrefix[Rdf]
  val rdfs = RDFSPrefix[Rdf]

  def instanceLabels(list: Seq[Rdf#URI])(implicit graph: Rdf#Graph): Seq[String] = list.map(instanceLabel)

  /**
   * display a summary of the resource (rdfs:label, foaf:name, etc,
   *  depending on what is present in instance and of the class) instead of the URI :
   *  TODO : this could use existing specifications of properties in form by class :
   *  ../forms/form_specs/foaf.form.ttl ,
   *  by taking the first one or two first literal properties.
   */
  def instanceLabel(uri: Rdf#URI)(implicit graph: Rdf#Graph): String = {
    val pgraph = PointedGraph(uri, graph)

    val firstName = (pgraph / foaf.firstName).as[String].getOrElse("")
    val lastName = (pgraph / foaf.lastName).as[String].getOrElse("")
    val n = firstName + " " + lastName
    if (n.size > 1) n
    else
      (pgraph / rdfs.label).as[String].
        getOrElse(
          (pgraph / foaf.name).as[String].
            getOrElse(
              // TODO : return RDF prefix
              uri.toString()))
  }
}
