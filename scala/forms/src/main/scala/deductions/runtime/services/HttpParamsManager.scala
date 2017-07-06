package deductions.runtime.services

import java.io.StringReader

import org.w3.banana._
import org.w3.banana.io.{NTriples, RDFReader, RDFWriter}

import scala.util.Try

/**
 * @author jmv
 */
trait HttpParamsManager[Rdf <: RDF] //extends RDFOpsModule
//    with NTriplesReaderModule
//    with NTriplesWriterModule 
{

  implicit val ops: RDFOps[Rdf]
  implicit val ntriplesReader: RDFReader[Rdf, Try, NTriples]
  implicit val ntriplesWriter: RDFWriter[Rdf, Try, NTriples]

  val tripleForHTTPParam = // false // 
    true

  import ops._

  def httpParam2Property(param: String) = {
    if (tripleForHTTPParam) {
      val tr = httpParam2Triple(param)
      fromUri(tr.predicate)
    } else param
  }

  /** leveraging on HTTP Parameter being the original triple from TDB,
   * in N-Triple syntax, we recover here the original triple. */
  def httpParam2Triple(param: String): Rdf#Triple = {
    val gr = ntriplesReader.read(new StringReader(param), "").get
    gr.triples.head
  }
  def triple2HttpParam(tr: Rdf#Triple) = {
    val gr = ops.makeGraph(Seq(tr))
    ntriplesWriter.asString(gr, "")
  }
}