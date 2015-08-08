package deductions.runtime.services

import org.w3.banana._
import java.io.StringReader

/**
 * @author jmv
 */
trait HttpParamsManager extends RDFOpsModule
    with NTriplesReaderModule
    with NTriplesWriterModule {

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