package deductions.runtime.jena

import org.w3.banana.jena.JenaModule
import deductions.runtime.services.DefaultConfiguration

object RDFLoaderApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with RDFCache with App
    with RDFStoreLocalJena1Provider {

  import ops._
  val uris = args map { p => URI(p) }
  uris map { readStoreUriInNamedGraph(_) }
  println(s"loaded +${uris.mkString("; ")}")
}