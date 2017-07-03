package deductions.runtime.services

import deductions.runtime.utils.HTTPrequest
import scala.xml.NodeSeq
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.algos.GeoPath
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.abstract_syntax.FormSyntaxFromSPARQL
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import deductions.runtime.html.TableView
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.html.HTML5TypesTrait

/**
 * global Controller for HTTP requests like /page?feature=dbpedia:CMS
 *  cf https://github.com/jmvanel/semantic_forms/issues/150
 */
trait CentralSemanticController[Rdf <: RDF, DATASET] extends SemanticController
    with RDFPrefixes[Rdf] {

  val actionMap: Map[String, SemanticController]

  def result(request: HTTPrequest): NodeSeq = {
    val features = request.queryString.getOrElse("feature", Seq())
    val res = for (
      featureAbbreviated <- features;
      // expand abbreviated URI's
      feature = expandOrUnchanged(featureAbbreviated)
    ) yield {
      val semanticController = actionMap.getOrElse(feature, NullSemanticController )
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
object NullSemanticController extends SemanticController {
  def result(request: HTTPrequest): NodeSeq = <div>NullSemanticController</div>
}

/** should be in first position in inheritance */
trait TypicalSFDependencies extends ImplementationSettings.RDFCache
with HTML5TypesTrait[ImplementationSettings.Rdf] {
    override implicit val config = new DefaultConfiguration {}
}
