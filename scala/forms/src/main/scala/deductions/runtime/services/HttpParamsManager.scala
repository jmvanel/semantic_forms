package deductions.runtime.services

import org.w3.banana._
import java.io.StringReader
import org.w3.banana.io.RDFReader
import org.w3.banana.io.NTriples
import scala.util.Try
import org.w3.banana.io.RDFWriter

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

  def httpParam2Triple(param: String): Rdf#Triple = {
    val gr = ntriplesReader.read(new StringReader(param), "").get
    gr.triples.head
  }
  def triple2HttpParam(tr: Rdf#Triple) = {
    val gr = ops.makeGraph(Seq(tr))
    ntriplesWriter.asString(gr, "")
  }
}