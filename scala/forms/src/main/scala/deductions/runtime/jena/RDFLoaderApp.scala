package deductions.runtime.jena

import org.w3.banana.jena.JenaModule
import deductions.runtime.services.DefaultConfiguration

object RDFLoaderApp extends RDFCache with App
    with RDFStoreLocalJena1Provider {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  import ops._
  val uris = args map { p => URI(p) }
  uris map { storeUriInNamedGraph(_) }
  println(s"loaded +${uris.mkString("; ")}")
}