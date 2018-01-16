package deductions.runtime.sparql_cache

import org.w3.banana.RDF
import scala.util.Try

trait MicrodataLoaderModule[Rdf <: RDF] {
  val microdataLoader: MicrodataLoader[Rdf]
}

trait MicrodataLoader[Rdf <: RDF] {
  def load(url: java.net.URL): Try[Rdf#Graph]  
}