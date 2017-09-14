package deductions.runtime.core

import scala.xml.NodeSeq

/**
 * Controller for HTTP requests like /page?feature=dbpedia:CMS
 *  cf https://github.com/jmvanel/semantic_forms/issues/150
 *  
 * **interface**
 */
trait SemanticController {
  
  val featureURI: String
  def result(request: HTTPrequest): NodeSeq
}

object NullSemanticController extends SemanticController {
  val featureURI = ""
  def result(request: HTTPrequest): NodeSeq = <div>NullSemanticController</div>
}
