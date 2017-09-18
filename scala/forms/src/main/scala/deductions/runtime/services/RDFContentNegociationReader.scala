package deductions.runtime.services

import scala.util.{ Failure, Success, Try }

import deductions.runtime.core.HTTPrequest
import org.w3.banana.io.RDFReader
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheDependencies

trait RDFContentNegociationReader[Rdf <: RDF, DATASET]
    extends RDFContentNegociation
    with RDFCacheDependencies[Rdf, DATASET] {

  def getRDFReader(request: HTTPrequest): RDFReader[Rdf, Try, Object] = {
    val mimeType = request.getHTTPheaderValue("Accept").getOrElse("")
    println( s">>>> getRDFReader: mimeType $mimeType")
    val rdfReader: RDFReader[Rdf, Try, Object] =
      foldRdfSyntax(mimeType, Unit)(
        _ => rdfXMLReader,
        _ => turtleReader,
        _ => jsonldReader)
      rdfReader
  }
}