package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.SaveListener
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDF
import scala.concurrent.Future

/** URI's Loader Listener, for form submit */
class URIsLoaderListenerClass(val config: Configuration)
  extends URIsLoaderListenerTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFModule
    with ImplementationSettings.RDFCache

trait URIsLoaderListenerTrait[Rdf <: RDF, DATASET]
  extends SaveListener[Rdf]
  with RDFCacheAlgo[Rdf, DATASET] {
  import ops._

  def notifyDataEvent(
    addedTriples:   Seq[Rdf#Triple],
    removedTriples: Seq[Rdf#Triple],
    request:        HTTPrequest,
    ipAdress:       String          = "",
    isCreation:     Boolean         = false)(implicit
    userURI: String,
    rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    for (tr <- addedTriples) {
      val uri = tr.objectt
      Future {
        retrieveURIBody(
          forceNodeToURI(uri), dataset,
          request,
          transactionsInside = true)
        logger.info(s"URIsLoaderListenerTrait: $uri was loaded.")
      }
    }
  }
}