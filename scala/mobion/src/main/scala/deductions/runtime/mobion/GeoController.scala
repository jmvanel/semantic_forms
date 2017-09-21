package deductions.runtime.mobion

import deductions.runtime.abstract_syntax.{ FormSyntaxFactory, FormSyntaxFromSPARQL }
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.html.TableView
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.RDFPrefixesInterface
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.SemanticController

import org.w3.banana.RDF

import scala.xml.NodeSeq

/** */
trait GeoController[Rdf <: RDF, DATASET] extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI]
    with RDFPrefixesInterface {

  import ops._
  override val featureURI: String = fromUri(geoloc("stats"))

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
        val detGraph = sparqlConstructQuery(detailsQuery).getOrElse(ops.emptyGraph);
        println(s"statisticsGraph size ${ops.graphSize(statisticsGraph)}");
        detGraph
      };
      formSyntax <- wrapInReadTransaction {
        createFormFromTriples(
          getTriples(
            union(Seq(statisticsGraph, detailsGraph))).toSeq,
          false)(allNamedGraph, request.getLanguage() );
      };
      _ = println(s"statisticsGraph formSyntax ${formSyntax}")
    ) yield {
      val html = generate(formSyntax)
      println(s"statisticsGraph html ${html.size}")
      html
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
