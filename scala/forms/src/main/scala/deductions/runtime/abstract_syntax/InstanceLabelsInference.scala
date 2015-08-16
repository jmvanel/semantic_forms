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
trait InstanceLabelsInference_Dupl[Rdf <: RDF] {
  self: PreferredLanguageLiteral[Rdf] =>
  val graph: Rdf#Graph
  val preferedLanguage: String

  import ops._
  val foaf = FOAFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]

  def instanceLabels(list: Seq[Rdf#Node]): Seq[String] = list.map(instanceLabel)

  /**
   * display a summary of the resource (rdfs:label, foaf:name, etc,
   *  depending on what is present in instance and of the class) instead of the URI :
   *  TODO : this could use existing specifications of properties in form by class :
   *  ../forms/form_specs/foaf.form.ttl ,
   *  by taking the first one or two first literal properties.
   */
  def instanceLabel(uri: Rdf#Node): String = {
    val pgraph = PointedGraph(uri, graph)

    val firstName = (pgraph / foaf.firstName).as[String].getOrElse("")
    val lastName = (pgraph / foaf.lastName).as[String].getOrElse("")
    val n = firstName + " " + lastName
    if (n.size > 1) n
    else {
      implicit val gr = graph
      implicit val prlng = preferedLanguage
      getLiteralInPreferedLanguageFromSubjectAndPredicate(uri, rdfs.label,
        getLiteralInPreferedLanguageFromSubjectAndPredicate(uri, foaf.name,
          uri.toString()))
      //  TODO : display RDF prefix
    }
  }
}
