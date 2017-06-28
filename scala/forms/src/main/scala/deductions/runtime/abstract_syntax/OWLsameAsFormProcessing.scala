package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/**
 * Automatically show inferred values from
 *  owl:sameAs axioms
 */
trait OWLsameAsFormProcessing[Rdf <: RDF, DATASET]
    extends GenericSPARQLformProcessing[Rdf, DATASET] {
  import ops._
  private val query: String =
    """|${declarePrefix("owl")}
       |CONTRUCT{
       | ?O ?PP ?OO .
       | ?S ?PPP ?OOO .
       |}
       |WHERE GRAPH {
       | ?GR {
       |    { <subject> owl:sameAs ?O .
       |      ?O ?PP ?OO .
       |    } UNION
       |      ?S owl:sameAs <subject> .
       |      ?S ?PPP ?OOO .
       |    }""".stripMargin

  def addOWLsameAs(formSyntax: FormSyntax): FormSyntax = {
    val subject = nodeToString(formSyntax.subject)
    val query2 = query.replaceAll("<subject>", subject)

    for (
      graph <- sparqlConstructQueryGraph(query2);
      _ = println(s"OWLsameAsFormProcessing: After sparqlConstructQueryGraph graph size ${graph.size}") ;
      tr <- getTriples(graph)
    ) {
      val entry = ResourceEntry(
        "label",
        "comment",
        property = tr.predicate,
        value = tr.objectt,
        valueLabel = "",
        type_ = nullURI,
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