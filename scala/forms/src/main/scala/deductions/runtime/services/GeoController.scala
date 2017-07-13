package deductions.runtime.services

import deductions.runtime.abstract_syntax.{ FormSyntaxFactory, FormSyntaxFromSPARQL }
import deductions.runtime.sparql_cache.dataset.RDFStoreLocalProvider
import deductions.runtime.html.TableView
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.HTTPrequest

import org.w3.banana.RDF

import scala.xml.NodeSeq

/** TODO extract to new SBT module */
trait GeoController[Rdf <: RDF, DATASET] extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI]
    with RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._

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

  def result(request: HTTPrequest): NodeSeq = {

    val statisticsGraphTry = wrapInReadTransaction {
      getPathLengthForAllMobiles(allNamedGraph)
    }

    val res = for (
      statisticsGraph <- statisticsGraphTry;
      detailsGraph <- wrapInReadTransaction {
        sparqlConstructQuery(detailsQuery).getOrElse(ops.emptyGraph)
      }
    ) yield {
      println(s"statisticsGraph size ${ops.graphSize(statisticsGraph)}")
      // create table view
      val formSyntax = createFormFromTriples(
        getTriples(
          union(Seq(statisticsGraph, detailsGraph))).toSeq,
        false)(allNamedGraph, "en")
      generate(formSyntax)
    }

    // load statistics in SF
    val res2 = wrapInTransaction {
      for (statisticsGraph <- statisticsGraphTry) {
        rdfStore.appendToGraph(dataset, URI("geoloc:stats"), statisticsGraph)
      }
    }

    val table = res.getOrElse(<div>Error! { res }</div>)
    table
  }
}