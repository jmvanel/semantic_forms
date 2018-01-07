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
//      def makeTTLURI(s: Rdf#Node) = s"<$s>"
//      def makeTTLBN(s: Rdf#Node) = s"_:$s"
//      def makeTTLAnyTerm(value: Rdf#Node) = {
//        foldNode(value)(
//          value => makeTTLURI(value),
//          bn => makeTTLBN(value),
//          lit =>
////          s""""${fromLiteral(lit)._1}""""
//            lit.toString()
//      )}

//      makeTTLURI(triple.subject) + " " +
//        makeTTLURI(triple.predicate) + " " +
//        makeTTLAnyTerm(triple.objectt) + " .\n"
      makeTurtleTerm(triple.subject) + " " +
      makeTurtleTerm(triple.predicate) + " " +
      makeTurtleTerm(triple.objectt) + " .\n"
    }
    URLEncoder.encode(rawResult.toString, "utf-8")
  }

}