package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes

/**
 * Take into account such annotations:
   bioc:Planting form:labelProperty bioc:species.
   (this allows to use ObjectProperty's like bioc:species for computing displayed labels)
 *  */
trait InstanceLabelsFromLabelProperty[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {

  import ops._

  /**
   * inferring possible label from:
   *
   * form:labelProperty in the rdf:type class
   */
  val query = s"""
		|${declarePrefix(form)}
    |SELECT ?LABEL_URI
    |WHERE {
    |  GRAPH ?G {
    |    ?CLASS form:labelProperty ?PROP.
    |  }
    |  GRAPH ?GG {
    |    <thing> a ?CLASS .
    |    <thing> ?PROP ?LABEL_URI.
    |} }
    """.stripMargin

  def instanceLabelFromLabelProperty(node: Rdf#Node): Option[Rdf#Node] = {
    ops.foldNode(node)(
      node => {
        if (node == ops.URI(""))
          None
        else {
          val q = query.replaceAll("\\<thing\\>", "<" + fromUri(node) + ">")
          // println(s"query $q")
          val res = for (
            nodes <- sparqlSelectQueryVariablesNT(q, List("LABEL_URI"));
            node <- nodes
          ) yield {
            node
          }
          res.headOption
        }
      },
      _ => None,
      _ => None)

  }
}