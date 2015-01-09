package deductions.runtime.sparql_cache

object RDFLoader extends RDFCache with App {
  import ops._
  val uris = args map { p => URI(p) }
  uris map { storeURI(_, dataset) }
  println(s"loaded +${uris.mkString("; ")}")
}