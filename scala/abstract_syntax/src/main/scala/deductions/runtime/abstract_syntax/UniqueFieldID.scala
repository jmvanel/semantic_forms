package deductions.runtime.abstract_syntax

import java.net.URLEncoder

import org.w3.banana.{RDF, RDFOps}
import deductions.runtime.utils.RDFHelpers
import org.w3.banana.io.RDFWriter
import scala.util.Try
import org.w3.banana.io.Turtle

trait UniqueFieldID[Rdf <: RDF] extends RDFHelpers[Rdf] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  val turtleWriter: RDFWriter[Rdf, Try, Turtle]

  /**
   * leveraging on HTTP parameter being the original triple from TDB,
   * in N-Triple syntax, we generate here the HTTP parameter from the original triple;
   * see HttpParamsManager#httpParam2Triple for the reverse operation
   */
  def makeHTMLName(triple: Rdf#Triple): String = {
    val rawResult = {
      val graph = makeGraph(List(triple).toIterable)
      val ttl = turtleWriter.asString(graph, "")
      if ( ttl.isFailure ) logger.warn(s"makeHTMLName: $ttl - $triple")
      ttl. getOrElse("")
//      makeTurtleTerm(triple.subject) + " " +
//      makeTurtleTerm(triple.predicate) + " " +
//      makeTurtleTerm(triple.objectt) + " .\n"
    }
    URLEncoder.encode(rawResult.toString, "utf-8")
  }

}