package deductions.runtime.services

import deductions.runtime.utils.HTTPrequest
import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.abstract_syntax.FormSyntaxFromSPARQL
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.html.TableView

/**
 * global Controller for HTTP requests like /page?feature=dbpedia:CMS
 *  cf https://github.com/jmvanel/semantic_forms/issues/150
 */
trait CentralSemanticController[Rdf <: RDF, DATASET] extends SemanticController
    with RDFPrefixes[Rdf] {
  /*  TODO in application config.
  val actionMap = Map("geoloc:stats" -> new GeoController[Rdf, DATASET]{}
  */
  val actionMap: Map[String, SemanticController]

  def result(request: HTTPrequest): NodeSeq = {
    val features = request.queryString.getOrElse("feature", Seq())
    val res = for (
      featureAbbreviated <- features;
      // expand abbreviated URI's
      feature = expandOrUnchanged(featureAbbreviated)
    ) yield {
      val semanticController = actionMap.getOrElse(feature, ???)
      semanticController.result(request)
    }
    res.flatten
  }
}

/**
 * Controller for HTTP requests like /page?feature=dbpedia:CMS
 *  cf https://github.com/jmvanel/semantic_forms/issues/150
 */
trait SemanticController {
  def result(request: HTTPrequest): NodeSeq
}


/** TODO extract to new SBT module */
trait GeoController[Rdf <: RDF, DATASET] extends GeoPath[Rdf, DATASET]
    with SemanticController
    with FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with TableView[Rdf#Node, Rdf#URI] {
  def result(request: HTTPrequest): NodeSeq = {
    val statisticsGraph = getPathLengthForAllMobiles(allNamedGraph)
    // create table view
    val formSyntax = createFormFromTriples(
      ops.getTriples(statisticsGraph).toSeq,
      false)(allNamedGraph, "en")
    generate(formSyntax)
  }
}