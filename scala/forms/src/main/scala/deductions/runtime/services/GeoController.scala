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

  import ops._

  def result(request: HTTPrequest): NodeSeq = {
    val res = wrapInReadTransaction {
      val statisticsGraph = getPathLengthForAllMobiles(allNamedGraph)
      println(s"statisticsGraph size ${ops.graphSize(statisticsGraph)}")
      val detailsQuery = """
        |${declarePrefix("geoloc")}
        |${declarePrefix("vehman")}
        |CONSTRUCT {
        |  ?MOB vehman:internalNumber ?NUM.
        |} WHERE {
        |  GRAPH ?GR {
        |    ?MOB a geoloc:Mobile.
        |  }
        |  GRAPH ?GR2 {
        |    ?MOB vehman:vehicle ?VEHICULE.
        |    ?VEHICULE vehman:internalNumber ?NUM.
        |  }
        |}""".stripMargin
      val detailsGraph = sparqlConstructQuery( detailsQuery ) . getOrElse(ops.emptyGraph)

      // create table view
      val formSyntax = createFormFromTriples(
        getTriples(
            union( Seq(statisticsGraph, detailsGraph))
        ).toSeq,
        false)(allNamedGraph, "en")
      generate(formSyntax)
    }
    val table = res.getOrElse(<div>Error! {res}</div>)
    table
  }
}