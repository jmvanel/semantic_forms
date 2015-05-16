package deductions.runtime.jena

import deductions.runtime.sparql_cache.RDFCache

object RDFLoaderApp extends RDFCache with App {
  import ops._
  val uris = args map { p => URI(p) }
  uris map { storeURI(_, dataset) }
  println(s"loaded +${uris.mkString("; ")}")
}