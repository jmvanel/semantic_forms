package deductions.runtime.jena

import deductions.runtime.sparql_cache.algos.ChildrenDocumentsFetcher
import java.net.URL

/** will be used for machine learning */
object ChildrenFetcherApp extends RDFStoreLocalJena1Provider with App
    with ChildrenDocumentsFetcher[ImplementationSettings.Rdf] {

  val url = args(0)
  val file = if (args.size > 1) args(1) else "dump2.nt"
  val triples = fetchDBPediaAbstractFromInterestsAndExpertise(new URL(url))
  writeToNTriplesFile(triples, file)

}