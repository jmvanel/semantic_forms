package deductions.runtime.jena

import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.RDFPrefixes

object RDFLoaderApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with RDFCache with App
    with ImplementationSettings.RDFCache // RDFStoreLocalJena1Provider
    with RDFPrefixes[ ImplementationSettings.Rdf] {

  import ops._
  val uris = args map { p => URI(expandOrUnchanged(p)) }
  uris map {
    uri =>
      readStoreUriInNamedGraph(uri)
      println(s"loaded <${uri}>")
  }
}