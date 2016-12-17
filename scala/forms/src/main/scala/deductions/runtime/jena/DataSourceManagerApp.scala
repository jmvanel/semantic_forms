package deductions.runtime.jena

import java.net.URL

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule

import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.sparql_cache.DataSourceManager

/** deductions.runtime.jena.DataSourceManagerApp */
object DataSourceManagerApp extends JenaModule
    with DataSourceManager[Jena, ImplementationSettings.DATASET] with App
    with RDFStoreLocalJena1Provider {

  val config = new DefaultConfiguration {}
  import config._

  val url: URL = new URL(args(0))
  val graphURI = args(1)
  implicit val graph: Rdf#Graph = allNamedGraph
  val num = replaceSameLanguageTriples(url: URL, graphURI: String)
  println(s"""Replaced Same Language triples from ${url} in graph $graphURI
      $num triples changed.""")
}