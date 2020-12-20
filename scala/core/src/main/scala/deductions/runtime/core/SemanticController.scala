package deductions.runtime.core

import scala.xml.NodeSeq
import scala.concurrent.Future

/**
 * Controller for HTTP requests like /page?feature=dbpedia:CMS
 *  cf https://github.com/jmvanel/semantic_forms/issues/150
 *
 * Can be used for any component that contributes to XHTML output
 * **interface**
 */
trait SemanticController extends SemanticControllerGeneric[NodeSeq]

/** Controller for HTTP requests like /rdf2json */
trait SemanticControllerGeneric[R] {
  val featureURI: String = ""
  def result(request: HTTPrequest): R
}

/** Future version of the above */
trait SemanticControllerFuture {
  val featureURI: String = ""
  def result(request: HTTPrequest): Future[NodeSeq]
}

object NullSemanticController extends SemanticController {
  def result(request: HTTPrequest): NodeSeq = <div>NullSemanticController</div>
}
