package deductions.runtime.abstract_syntax

import deductions.runtime.utils.{ RDFHelpers, RDFPrefixes }
import org.w3.banana.{ PointedGraph, RDF }

import scala.collection.Seq
import scalaz._
import Scalaz._

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
    if (node == nullURI || ! (isURI(node) || isBN(node))) return ""
    //    println("**** makeInstanceLabel " + find( graph, node, foaf.givenName, ANY) . toList )

    val pgraph = PointedGraph(node, graph)
    def valueForProp(prop: Rdf#URI): String = {
      val pg = (pgraph / prop)
      val y = pg.as[Rdf#Literal].getOrElse(Literal(""))
      fromLiteral(y)._1
    }

    def tryFoafPerson() = try2Properties(foaf.givenName, foaf.familyName)
    def tryFoafPerson2() = try2Properties(foaf.firstName, foaf.lastName)
    def tryNonStandardPerson() = try2Properties(
      prefixesMap2("pair")("firstName"),
      prefixesMap2("pair")("lastName"))

    def try2Properties(prop1: Rdf#URI, prop2: Rdf#URI) = {
      val givenName = valueForProp(prop1)
      val familyName = valueForProp(prop2)
      val fullName = givenName + " " + familyName
      if (fullName.size > 1)
        fullName
      else ""
    }
    val pairPreferedLabelProp = prefixesMap2("pair")("preferedLabel")

    val n = tryFoafPerson()
    if (!n.isEmpty()) n
    else {
      val fullName = tryFoafPerson2()
      if (!fullName.isEmpty())
        fullName
      else {
        val fullName = tryNonStandardPerson
        if (!fullName.isEmpty())
          fullName
        else {
        implicit val gr = graph
        implicit val prlng = lang

        val l = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, rdfs.label, "")
        if (l  =/=  "") return l
        val n = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, foaf.name, "")
        if (n  =/=  "") return n
        val s = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, skos("prefLabel"), "")
        if (s  =/=  "") return s
        val dctitle = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, dc("title"), "")
        if (dctitle  =/=  "") return dctitle
        val dcttitle = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, dct("title"), "")
        if (dcttitle  =/=  "") return dcttitle
        val geoname = getLiteralInPreferedLanguageFromSubjectAndPredicate(node,
            URI("http://www.geonames.org/ontology#name"), "")
        if (geoname  =/=  "") return geoname
        val pairPreferedLabel = getLiteralInPreferedLanguageFromSubjectAndPredicate(node, pairPreferedLabelProp, "")
        if (pairPreferedLabel  =/=  "") return pairPreferedLabel

        //        val cl = instanceClassLabel( node, graph, lang)
        ////        println( s"""instanceClassLabel $node "$cl" """ )
        //        if (cl  =/=  "") return cl
        last_segment(node)
      }
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
        val label = getLiteralInPreferedLanguageFromSubjectAndPredicate(
          classs,
          rdfs.label, lsegment)
        if (label === "Thing")
          ""
        else
          label
      case None => lsegment
    }
  }
}
