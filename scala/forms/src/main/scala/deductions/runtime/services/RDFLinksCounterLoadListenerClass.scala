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
class RDFLinksCounterLoadListenerClass(val config: Configuration,
    rdfLocalProvider: RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET] )
    extends ServiceListener[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFModule

    // TODO remove
    with ImplementationSettings.RDFCache

    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  def notifyServiceCall(
    request: HTTPrequest)(
      implicit userURI: String,
      rdfLocalProvider: RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]): Unit = {
    println(s"\nnotifyServiceCall: ${request.rawQueryString} - dataset ${this.rdfLocalProvider.dataset}")
    val datasetUsed =
      // this.rdfLocalProvider.dataset
      rdfLocalProvider.dataset
    for (
      uri <- request.getHTTPparameterValue("displayuri") if (request.path == "/display");
      uri1 = expandOrUnchanged(uri);
      graph <- retrieveURIBody(ops.URI(uri1),
        datasetUsed,
        request,
        transactionsInside = true);
      _ = println(s"  notifyServiceCall: URI $uri1 graph $graph")
    ) {
      val addedTriples = ops.getTriples(graph).toSeq
      println( s"  >>>> notifyServiceCall: addedTriples ${addedTriples.size}")
      updateLinksCount(
        databaseChanges = DatabaseChanges[Rdf](addedTriples, Seq()),
        linksCountDataset = datasetUsed,
        linksCountGraphURI = defaultLinksCountGraphURI,
        replaceCount = true)
    }
    println(s"notifyServiceCall ENDED: ${request.rawQueryString}\n")
  }

}