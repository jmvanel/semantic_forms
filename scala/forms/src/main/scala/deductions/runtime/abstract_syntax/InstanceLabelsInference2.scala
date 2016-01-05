package deductions.runtime.abstract_syntax

import scala.collection.Seq

import org.w3.banana.FOAFPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix

/**
 * populate Fields in form by inferring possible label from:
 * FOAF Person properties
 * foaf:name
 * rdfs.label
 * rdfs.label from class ( by rdf:type)
 * 
 * systematically trying to get the matching language form,
 * and as last fallback, the last segment of the URI.
 */
private[abstract_syntax] trait InstanceLabelsInference2[Rdf <: RDF] {
  self: PreferredLanguageLiteral[Rdf] =>

  import ops._
  private lazy val foaf = FOAFPrefix[Rdf]
  private lazy val rdfs = RDFSPrefix[Rdf]
  private lazy val rdf = RDFPrefix[Rdf]

  def instanceLabels(list: Seq[Rdf#Node], lang: String = "")(implicit graph: Rdf#Graph): Seq[String] =
    list.map { node => instanceLabel(node, graph, lang) }

  /**
   * display a summary of the resource (rdfs:label, foaf:name, etc,
   *  depending on what is present in instance and of the class) instead of the URI :
   *  TODO : this could use existing specifications of properties in form by class :
   *  ../forms/form_specs/foaf.form.ttl ,
   *  by taking the first one or two first literal properties.
   *
   *  NON transactional
   */
  def instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    val pgraph = PointedGraph(node, graph)

    val firstName = (pgraph / foaf.firstName).as[String].getOrElse("")
    val lastName = (pgraph / foaf.lastName).as[String].getOrElse("")

    val n = firstName + " " + lastName
    if (n.size > 1) n
    else {
      val givenName = (pgraph / foaf.givenName).as[String].getOrElse("")
      val familyName = (pgraph / foaf.familyName).as[String].getOrElse("")
      val n = givenName + " " + familyName
      if (n.size > 1) n
      else {
        implicit val gr = graph
        implicit val prlng = lang

        def last_segment(node: Rdf#Node) =
          try {
            foldNode(node)(
              uri => lastSegment(uri),
//              bn => instanceClassLabel(bn, graph, lang),
              bn => bn.toString(),
              x => x.toString())
          } catch {
            case t: Throwable => node.toString()
          }

        def instanceClassLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
          val pgraph = PointedGraph(node, graph)
          val noption = (pgraph / rdf.typ).nodes.headOption
          noption match {
            case Some(classs) =>
              getLiteralInPreferedLanguageFromSubjectAndPredicate(classs,
                rdfs.label, last_segment(node))
            case None => last_segment(node)
          }
        }

        val l = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, rdfs.label, "")
        if (l != "") return l
        val n = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, foaf.name, "")
        if (n != "") return n
        val cl = instanceClassLabel( node, graph, lang)
        if (cl != "") return cl
        last_segment(node)
      }
    }
  }
}
