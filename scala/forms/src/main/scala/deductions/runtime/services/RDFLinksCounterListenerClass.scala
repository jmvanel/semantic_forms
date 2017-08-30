package deductions.runtime.services

import deductions.runtime.core.HTTPrequest
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.rdf_links_rank.RDFLinksCounter
import deductions.runtime.utils.Configuration
import deductions.runtime.utils.DatabaseChanges
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.SaveListener

/** RDF Links Counter Listener, for form submit */
class RDFLinksCounterListenerClass(val config: Configuration)
    extends SaveListener[ImplementationSettings.Rdf]
    with ImplementationSettings.RDFModule
    with ImplementationSettings.RDFCache
    with RDFLinksCounter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  def notifyDataEvent(
    addedTriples: Seq[Rdf#Triple],
    removedTriples: Seq[Rdf#Triple],
    request: HTTPrequest,
    ipAdress: String = "",
    isCreation: Boolean = false)(implicit userURI: String,
        rdfLocalProvider: RDFStoreLocalProvider[Rdf, _]): Unit = {

    updateLinksCount(
      databaseChanges = DatabaseChanges[Rdf](addedTriples, removedTriples),
      linksCountDataset = dataset,
      linksCountGraphURI = ops.URI("linksCountGraph:"))
  }
}