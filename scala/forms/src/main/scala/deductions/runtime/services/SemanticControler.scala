package deductions.runtime.services

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.html.HTML5TypesTrait
import deductions.runtime.utils.{DefaultConfiguration, HTTPrequest, RDFPrefixes}
import org.w3.banana.RDF

import scala.xml.NodeSeq

/**
 * global Controller for HTTP requests like /page?feature=dbpedia:CMS
 * calls the result() function of the class mapped to feature URI by #actionMap;
 * see trait GeoController as a example implemetation of SemanticController
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
with HTML5TypesTrait[ImplementationSettings.Rdf]
with DefaultConfiguration{
    override implicit val config = new DefaultConfiguration {}
}
