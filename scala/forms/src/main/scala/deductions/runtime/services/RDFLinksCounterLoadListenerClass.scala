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
import scala.concurrent.Future
import java.util.concurrent.TimeUnit

/** RDF Links Counter Listener, for RDF document loading */
class RDFLinksCounterLoadListenerClass(val config: Configuration,
    rdfLocalProvider: RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET] )
    extends ServiceListener[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ImplementationSettings.RDFModule

    // TODO remove
    with ImplementationSettings.RDFCache

    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with RDFCacheAlgo[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  def notifyServiceCall(
    request: HTTPrequest)(
    implicit
    userURI:          String,
    rdfLocalProvider: RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]): Unit = {
    val datasetUsed = rdfLocalProvider.dataset // this.rdfLocalProvider.dataset

    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      println(s"\nRDFLinksCounterLoadListenerClass notifyServiceCall: ${request.rawQueryString} - dataset ${datasetUsed}")
      TimeUnit.MILLISECONDS.sleep(500)
      for (
        uri <- request.getHTTPparameterValue("displayuri") if (request.path == "/display");
        uri1 = expandOrUnchanged(uri);
        graph = {
          val graph = checkIfNothingStoredLocally(ops.URI(uri1), transactionsInside = true)._2;
          println(s"  notifyServiceCall: URI $uri1 graph")
          graph
        };
        //      retrieveURIBody(ops.URI(uri1),
        addedTriples <- wrapInReadTransaction {
          val triples = getTriples(graph).toSeq
          println(s"  >>>> RDFLinksCounterLoadListenerClass notifyServiceCall: addedTriples ${triples.size}")
          triples
        }
      ) {
        updateLinksCount(
          databaseChanges = DatabaseChanges[Rdf](addedTriples, Seq()),
          linksCountDataset = datasetUsed,
          linksCountGraphURI = defaultLinksCountGraphURI,
          replaceCount = true)
      }
      println(s"RDFLinksCounterLoadListenerClass notifyServiceCall ENDED: ${request.rawQueryString}\n")
    }
  }

}