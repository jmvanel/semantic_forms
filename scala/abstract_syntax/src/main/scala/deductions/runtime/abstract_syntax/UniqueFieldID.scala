package deductions.runtime.abstract_syntax

import java.net.URLEncoder

import org.w3.banana.{RDF, RDFOps}
import deductions.runtime.utils.RDFHelpers

trait UniqueFieldID[Rdf <: RDF] extends RDFHelpers[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._

  /**
   * leveraging on HTTP parameter being the original triple from TDB,
   * in N-Triple syntax, we generate here the HTTP parameter from the original triple;
   * see HttpParamsManager#httpParam2Triple for the reverse operation
   */
  def makeHTMLName(triple: Rdf#Triple): String = {
    val rawResult = {
      val ttl = makeTurtleTriple(triple)
      if ( ttl.isFailure ) logger.warn(s"makeHTMLName: $ttl - triple : $triple")
      ttl. getOrElse("")
    }
    URLEncoder.encode(rawResult.toString, "utf-8")
  }

}