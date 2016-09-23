package deductions.runtime.jena

import org.w3.banana.jena.JenaModule

object RDFLoaderApp extends RDFCache with App
    with RDFStoreLocalJena1Provider {
  import ops._
  val uris = args map { p => URI(p) }
  uris map { storeUriInNamedGraph(_) }
  println(s"loaded +${uris.mkString("; ")}")
}