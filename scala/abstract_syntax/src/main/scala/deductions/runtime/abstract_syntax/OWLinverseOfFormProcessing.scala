package deductions.runtime.abstract_syntax

import deductions.runtime.core.{URIWidget, zeroOrMore}
import org.w3.banana.RDF

/**
 * Automatically show inferred values from
 *  owl:inverseOf axioms
 *  see https://www.w3.org/TR/2004/REC-owl-guide-20040210/#inverseOf
 */
trait OWLinverseOfFormProcessing[Rdf <: RDF, DATASET]
    extends GenericSPARQLformProcessing[Rdf, DATASET] {
  import ops._
  private val query: String =
    """|CONTRUCT{ ?S ?P ?O}
  |WHERE GRAPH {
  |?GR { ?S ?P ?O} } LIMIT 22""".stripMargin

  def addOWLinverseOf(formSyntax: FormSyntax): FormSyntax = {
    val subject = nodeToString(formSyntax.subject)

    for (
      graph <- sparqlConstructQueryGraph(query);
      tr <- getTriples(graph)
    ) {
      val entry = ResourceEntry(
        "label",
        "comment",
        property = tr.predicate,
        value = tr.objectt,
        valueLabel = "",
        type_ = Seq(), // nullURI,
        inverseTriple = false,
        subject = tr.subject,
        subjectLabel = "",
        mandatory = false,
        openChoice = true,
        widgetType = URIWidget,
        cardinality = zeroOrMore,
        isImage = false,
        thumbnail = None,
        htmlName = "",
        metadata = "",
        timeMetadata = -1)
      formSyntax.fields = formSyntax.fields :+ entry
    }
    formSyntax
  }
}