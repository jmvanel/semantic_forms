package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFOps

/**
 * @author jmv
 */
trait PreferredLanguageLiteral[Rdf <: RDF] //extends RDFOpsModule
{
  implicit val ops: RDFOps[Rdf]
  //  self: RDFOpsModule =>

  import ops._

  /**
   * get value in preferred Language From values (objects of triples) Or Else given default;
   *  use application language
   */
  def getPreferedLanguageFromSubjectAndPredicate(subject: Rdf#Node, predicate: Rdf#URI, default: String)(implicit graph: Rdf#Graph, preferedLanguage: String = "en"): String = {
    //    println("getPreferedLanguageFromSubjectAndPredicate: " + subject + " " + predicate) // debug
    ops.getObjects(graph, subject, predicate) match {
      case ll if ll == Set.empty => default
      case ll => getPreferedLanguageLiteral(ll)
    }
  }

  /** get preferred Language value From RDF Values that are language marked or not */
  private def getPreferedLanguageLiteral(values: Iterable[Rdf#Node])(implicit graph: Rdf#Graph, preferedLanguage: String = "en"): String = {
    def computeValues(): (String, String, String) = {
      var preferedLanguageValue = ""
      var enValue = ""
      var noLanguageValue = ""
      for (value <- values) {
        foldNode(value)(
          x => (), x => (),
          value => {
            val tt = fromLiteral(value)
            val (raw, uri, langOption) = fromLiteral(value)
            // println("getPreferedLanguageFromValues: " + (raw, uri, langOption) )
            langOption match {
              case Some(language) =>
                if (language == preferedLanguage) preferedLanguageValue = raw
                else if (language == "en")
                  enValue = raw
              case None => noLanguageValue = raw
              case _ =>
            }
          }
        )
      }
      //      println(s"preferedLanguageValue: $preferedLanguageValue , enValue $enValue, noLanguageValue $noLanguageValue")
      (preferedLanguageValue, enValue, noLanguageValue)
    }
    val (preferedLanguageValue, enValue, noLanguageValue) = computeValues
    (preferedLanguageValue, enValue, noLanguageValue) match {
      case _ if (preferedLanguageValue != "") => preferedLanguageValue
      case _ if (enValue != "") => enValue
      case _ if (noLanguageValue != "") => noLanguageValue
      case _ =>
        val mess = s"preferedLanguageValue $preferedLanguageValue, enValue $enValue, noLanguageValue $noLanguageValue"
        println(s"getPreferedLanguageFromValues: case not expected in values ${values.mkString(", ")}: $mess"); "en"
    }
  }
}