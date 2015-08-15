package deductions.runtime.abstract_syntax

import scala.collection.Seq
import org.w3.banana.FOAFPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFOps

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 *  TODO : duplicated code with InstanceLabelsInference
 */
trait InstanceLabelsInference2[Rdf <: RDF] //        extends RDFOpsModule //    
//    extends PreferredLanguageLiteral[Rdf]
{
  self: PreferredLanguageLiteral[Rdf] =>

  //  implicit val ops: RDFOps[Rdf]

  import ops._
  private lazy val foaf = FOAFPrefix[Rdf]
  lazy val rdfs = RDFSPrefix[Rdf]
  lazy val rdf = RDFPrefix[Rdf]

  def instanceLabels(list: Seq[Rdf#URI], lang: String = "")(implicit graph: Rdf#Graph): Seq[String] =
    list.map { uri => instanceLabel(uri, graph, lang) }

  /**
   * display a summary of the resource (rdfs:label, foaf:name, etc,
   *  depending on what is present in instance and of the class) instead of the URI :
   *  TODO : this could use existing specifications of properties in form by class :
   *  ../forms/form_specs/foaf.form.ttl ,
   *  by taking the first one or two first literal properties.
   *  TODO : take in account a preferred language like getPreferedLanguageFromSubjectAndPredicate does,
   */
  def instanceLabel(uri: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    //  def instanceLabel(uri: Rdf#Node, lang: String = "")(implicit graph: Rdf#Graph): String = {
    val pgraph = PointedGraph(uri, graph)
    // println(s"instanceLabel pgraph ${pgraph.pointer} ${pgraph.graph}")
    val firstName = (pgraph / foaf.firstName).as[String].getOrElse("")
    val lastName = (pgraph / foaf.lastName).as[String].getOrElse("")

    val n = firstName + " " + lastName
    val r = if (n.size > 1) n // Literal(n)
    else {
      val givenName = (pgraph / foaf.givenName).as[String].getOrElse("")
      val familyName = (pgraph / foaf.familyName).as[String].getOrElse("")
      val n = givenName + " " + familyName
      if (n.size > 1) n // Literal(n)
      else {
        implicit val gr = graph
        implicit val prlng = lang
        getLiteralInPreferedLanguageFromSubjectAndPredicate(uri, rdfs.label,
          getLiteralInPreferedLanguageFromSubjectAndPredicate(uri, foaf.name,
            //        val rr = (pgraph / rdfs.label).as[Rdf#Literal].
            //          getOrElse {
            //            (pgraph / foaf.name).as[Rdf#Literal].
            //              getOrElse
            {
              val classLabel = (pgraph / rdf.typ / rdfs.label).as[Rdf#Literal].
                getOrElse(Literal(""))
              if (classLabel != Literal(""))
                ("a " + classLabel.lexicalForm)
              else
                // TODO : return Turtle prefix
                (uri.toString())
            }
          ))
      }
      //        rr
    }
    //    }
    r // .lexicalForm
  }
}
