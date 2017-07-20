package deductions.runtime.abstract_syntax.uri

import org.w3.banana.RDF
import deductions.runtime.core.URIForDisplay
import org.w3.banana.binder.FromURI
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory

trait URIForDisplayFactory[Rdf <: RDF, DATASET] {
  self: InstanceLabelsInferenceMemory[Rdf, DATASET] =>

  import ops._

  /** make a data structure For Display from an URI */
  def makeURIForDisplay(uri: Rdf#Node)(implicit graph: Rdf#Graph,
                                      lang: String = "en"): URIForDisplay = {

    URIForDisplay(
      fromUri( uriNodeToURI(uri) ),
      makeInstanceLabel(uri, graph, lang),
      // TODO <<<<<<<<<<<<<<<
      typ = "???",
      typeLabel = "???",
      thumbnail = None,
      isImage = false)

  }
}