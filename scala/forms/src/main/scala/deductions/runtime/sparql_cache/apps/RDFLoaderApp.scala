package deductions.runtime.sparql_cache.apps

import deductions.runtime.jena.{ ImplementationSettings, RDFCache }
import deductions.runtime.utils.{ DefaultConfiguration, RDFPrefixes }

/**
 * download & Store URI's In self-Named Graph;
 * remove previous Graph content
 * Text indexing = true
 *
 * like tdb2.tdbloader
 */
object RDFLoaderApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = true
  }
} with RDFCache with App
  with ImplementationSettings.RDFCache
  with RDFPrefixes[ImplementationSettings.Rdf] {

  import ops._
  val uris = args map { p => URI(expandOrUnchanged(p)) }
  uris map {
    uri =>
      rdfStore.removeGraph(dataset, uri)
      println(s"loading <${uri}>")
      readStoreUriInNamedGraph(uri)
      println(s"loaded  <${uri}>")
  }
  closeAllTDBs()
}

/**
 * download & Store URL In given Graph;
 * remove previous Graph content
 * Text indexing = false
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
  closeAllTDBs()
}