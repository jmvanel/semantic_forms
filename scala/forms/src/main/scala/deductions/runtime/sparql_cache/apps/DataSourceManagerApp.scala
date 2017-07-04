package deductions.runtime.sparql_cache.apps

import java.net.URL

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.DataSourceManager
import org.w3.banana.jena.Jena

/** deductions.runtime.jena.DataSourceManagerApp */
object DataSourceManagerApp extends {
  override val config = new DefaultConfiguration {
  }
} with ImplementationSettings.RDFModule
    with DataSourceManager[Jena, ImplementationSettings.DATASET] with App
    with ImplementationSettings.RDFCache {

  val url: URL = new URL(args(0))
  val graphURI = args(1)
  implicit val graph: Rdf#Graph = allNamedGraph
  val num = replaceSameLanguageTriples(url: URL, graphURI: String)
  println(s"""Replaced Same Language triples from ${url} in graph $graphURI
      $num triples changed.""")
}