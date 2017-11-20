package deductions.runtime.sparql_cache.apps

import deductions.runtime.jena.{ ImplementationSettings, RDFCache }
import deductions.runtime.utils.{ DefaultConfiguration, RDFPrefixes }

/**
 * download & Store URL In given Graph;
 * remove previous Graph content
 *  
 * like tdb2.tdbloader
 */
object RDFLoaderGraphApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with RDFCache with App
  with ImplementationSettings.RDFCache
  with RDFPrefixes[ImplementationSettings.Rdf] {

  import ops._
  val uri = URI(expandOrUnchanged(args(0)))
  val graph = URI(args(1))
  rdfStore.removeGraph(dataset, graph)
  readStoreURI(uri, graph, dataset)
  println(s"loaded <${uri}> in graph <${graph}>")
}