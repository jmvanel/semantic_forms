package deductions.runtime.sparql_cache.apps

import deductions.runtime.sparql_cache.algos.ChildrenDocumentsFetcher
import java.net.URL
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

/** will be used for machine learning */
object ChildrenFetcherApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFCache with App
    with ChildrenDocumentsFetcher[ImplementationSettings.Rdf] {

  //  val config = new DefaultConfiguration {
  //    override val useTextQuery = false
  //  }
  import config._

  val url = args(0)
  val file = if (args.size > 1) args(1) else "dump2.nt"
  val triples = fetchDBPediaAbstractFromInterestsAndExpertise(new URL(url))
  writeToNTriplesFile(triples, file)

}