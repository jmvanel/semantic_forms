package deductions.runtime.abstract_syntax

import scala.language.postfixOps

import org.w3.banana.RDF
import org.w3.banana.RDFOps

/**
 * @author jmv
 */
trait PreferredLanguageLiteral[Rdf <: RDF] {
  implicit val ops: RDFOps[Rdf]

  import ops._

  /**
   * get value in preferred Language From values (objects of triples) Or Else given default;
   *  use application language
   */
  def getLiteralInPreferedLanguageFromSubjectAndPredicate(subject: Rdf#Node,
                                                          predicate: Rdf#Node, default: String)(implicit graph: Rdf#Graph,
                                                                                                lang: String = "en"): String = {
    // println("getPreferedLanguageFromSubjectAndPredicate: " + subject + " " + predicate + s" lang $lang") // debug
    foldNode(predicate)(
      predURI =>
        getObjects(graph, subject, predURI).toList match {
          case ll if ll isEmpty => default
          case ll               => getPreferedLanguageLiteral(ll)
        },
      _ => "", _ => "")
  }

  /** get preferred Language value From RDF Values that are language marked or not */
  private def getPreferedLanguageLiteral(values: Iterable[Rdf#Node])(implicit graph: Rdf#Graph,
    lang: String = "en"): String = {
//      println(s""">>>> getPreferedLanguageLiteral "$lang" values $values""")

    def computeValues(): (String, String, String) = {
      var preferedLanguageValue = ""
      var enValue = ""
      var noLanguageValue = ""
      for (value <- values) {
        foldNode(value)(
          x => (), x => (),
          value => {
            val (raw, uri, langOption) = fromLiteral(value)
            // println("getPreferedLanguageFromValues: " + (raw, uri, langOption) )
            langOption match {
              case Some(language) =>
                if (language == lang) preferedLanguageValue = raw
                else if (language == "en")
                  enValue = raw
              case None => noLanguageValue = raw
              case _ =>
            }
          }
        )
      }
      (preferedLanguageValue, enValue, noLanguageValue)
    }

    if (values.size == 1)
      return foldNode(values.head)(
        _ => "", _ => "",
        lit => fromLiteral(lit)._1)

    val (preferedLanguageValue, enValue, noLanguageValue) = computeValues
    (preferedLanguageValue, enValue, noLanguageValue) match {
      case _ if (preferedLanguageValue != "") => preferedLanguageValue
      case _ if (enValue != "") => enValue
      case _ if (noLanguageValue != "") => noLanguageValue
      case _ =>
        val mess = s"""preferedLanguageValue "$preferedLanguageValue", enValue "$enValue"", noLanguageValue "$noLanguageValue""""
        println(
          s"getPreferedLanguageFromValues: case not expected in values ${values.mkString(", ")}: $mess");
        "no value"
    }
  }
}