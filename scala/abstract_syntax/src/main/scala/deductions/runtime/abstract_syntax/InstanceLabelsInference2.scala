package deductions.runtime.abstract_syntax

import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import org.w3.banana.{PointedGraph, RDF}

import scala.collection.Seq

/**
 * populate Fields in form by inferring possible label from:
 * FOAF Person properties
 * foaf:name
 * rdfs.label
 * rdfs.label from class ( by rdf:type )
 * form:labelProperty in the rdf:type (in InstanceLabelsFromLabelProperty )
 * 
 * systematically trying to get the matching language form,
 * and as last fallback, the last segment of the URI.
 */
//private[abstract_syntax] 
trait InstanceLabelsInference2[Rdf <: RDF]
		extends RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {
  self: PreferredLanguageLiteral[Rdf] =>

  import ops._

  def instanceLabels(list: Seq[Rdf#Node], lang: String = "")(implicit graph: Rdf#Graph): Seq[String] =
    list.map { node => makeInstanceLabel(node, graph, lang) }

  /**
   * display a summary of the resource (rdfs:label, foaf:name, etc,
   *  depending on what is present in instance and of the class) instead of the URI :
   *  TODO : this could use existing specifications of properties in form by class :
   *  ../forms/form_specs/foaf.form.ttl ,
   *  by taking the first one or two first literal properties.
   *
   *  NON transactional
   */
  def makeInstanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    if (node == nullURI) return ""
    println("**** makeInstanceLabel" + find( graph, node, foaf.givenName, ANY) . toList )

    val pgraph = PointedGraph(node, graph)
    def valueForProp(prop: Rdf#URI): String = {
       val pg = (pgraph / prop)
       val y = pg . as[Rdf#Literal].getOrElse(Literal(""))
       fromLiteral(y)._1
      // (pgraph / prop).as[String].getOrElse("")
    }

    val givenName = valueForProp(foaf.givenName)
    val familyName = valueForProp(foaf.familyName)
    val n = givenName + " " + familyName

    if (n.size > 1) n
    else {
      val firstName = valueForProp(foaf.firstName)
      val lastName = valueForProp(foaf.lastName)

      val n = givenName + " " + familyName
      if (n.size > 1) n
      else {
        implicit val gr = graph
        implicit val prlng = lang

        val l = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, rdfs.label, "")
        if (l != "") return l
        val n = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, foaf.name, "")
        if (n != "") return n
        val s = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, skos("prefLabel"), "")
        if (s != "") return s

        //        val cl = instanceClassLabel( node, graph, lang)
        ////        println( s"""instanceClassLabel $node "$cl" """ )
        //        if (cl != "") return cl
        last_segment(node)
      }
    }
  }

  /** unused */
  private def instanceClassLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    val pgraph = PointedGraph(node, graph)
    val noption = (pgraph / rdf.typ).nodes.headOption
    val lsegment = last_segment(node)
    noption match {
      case Some(classs) =>
        implicit val gr: Rdf#Graph = graph
        val label = getLiteralInPreferedLanguageFromSubjectAndPredicate(classs,
          rdfs.label, lsegment)
        if (label == "Thing")
          ""
        else
          label
      case None => lsegment
    }
  }
}
