package deductions.runtime.services

import deductions.runtime.rdf_links_rank.RDFLinksCounter

import deductions.runtime.utils.SaveListener
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.DatabaseChanges
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.ServiceListener
import deductions.runtime.utils.RDFStoreLocalProvider

import deductions.runtime.sparql_cache.RDFCacheAlgo

/** RDF Links Counter Listener, for RDF document loading */
class RDFLinksCounterLoadListenerClass(val config: Configuration)
    extends ServiceListener[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFModule
    with ImplementationSettings.RDFCache
    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  def notifyServiceCall(
    request: HTTPrequest)(
      implicit userURI: String,
      rdfLocalProvider: RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]): Unit = {
    for (
      uri <- request.getHTTPparameterValue("displayuri") if (request.path == "/display");
      uri1 = expandOrUnchanged(uri);
      graph <- retrieveURIBody(ops.URI(uri1), rdfLocalProvider.dataset,
        request,
        transactionsInside = true)
    ) {
      val addedTriples = ops.getTriples(graph).toSeq
//      println( s">>>> notifyServiceCall: addedTriples $addedTriples")
      updateLinksCount(
        databaseChanges = DatabaseChanges[Rdf](addedTriples, Seq()),
        linksCountDataset = dataset,
        linksCountGraphURI = ops.URI("linksCountGraph:"))
    }
  }

}