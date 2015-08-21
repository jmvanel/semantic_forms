package deductions.runtime.jena

import org.w3.banana.jena.JenaModule
import deductions.runtime.sparql_cache.DataSourceManager
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import java.net.URL
import org.w3.banana.jena.JenaMGraphOps

/** deductions.runtime.jena.DataSourceManagerApp */
object DataSourceManagerApp extends JenaModule
    with DataSourceManager[Jena, Dataset] with App
    with RDFStoreLocalJena1Provider //    with JenaHelpers
    {

  import ops._
  val url: URL = new URL(args(0))
  val graphURI = args(1)
  val num = replaceSameLanguageTriples(url: URL, graphURI: String)
  println(s"""Replaced Same Language triples from ${url} in graph $graphURI
      $num triples changed.""")
}