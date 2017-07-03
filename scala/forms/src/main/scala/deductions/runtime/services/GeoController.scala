package deductions.runtime.services

import scala.xml.NodeSeq

import org.w3.banana.RDF

import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.abstract_syntax.FormSyntaxFromSPARQL
import deductions.runtime.html.TableView
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.HTTPrequest

/** TODO extract to new SBT module */
trait GeoController[Rdf <: RDF, DATASET] extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI] {

  def result(request: HTTPrequest): NodeSeq = {
    val res = wrapInReadTransaction {
      val statisticsGraph = getPathLengthForAllMobiles(allNamedGraph)
      // create table view
      val formSyntax = createFormFromTriples(
        ops.getTriples(statisticsGraph).toSeq,
        false)(allNamedGraph, "en")
      generate(formSyntax)
    }
    val table = res.getOrElse(<div>Error! </div>)
    table
  }
}