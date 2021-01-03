package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.SaveListener
import deductions.runtime.sparql_cache.RDFCacheAlgo
import org.w3.banana.RDF
import scala.concurrent.Future
import deductions.runtime.utils.RDFHelpers

/** URI's Loader Listener, for form submit */
class URIsLoaderListenerClass(val config: Configuration)
  extends 
     ImplementationSettings.RDFModule with
  URIsLoaderListenerTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFCache
    with RDFHelpers[ImplementationSettings.Rdf]

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
      if (isURI(uri)) {
        Future {
          retrieveURIBody(
            forceNodeToURI(uri), dataset,
            request,
            transactionsInside = true)
          logger.info(s"URIsLoaderListenerTrait: <$uri> was loaded (when subject <${tr.subject}> was updated).")
        }
      }
//      else {
//        println(s"URIsLoaderListenerTrait notifyDataEvent: NOT an URI : $uri")
//      }
    }
  }
}