package deductions.runtime.abstract_syntax

import deductions.runtime.sparql_cache.SPARQLHelpers
import org.w3.banana.RDF

/** a step in Form generation */
trait GenericFormProcessing[Rdf <: RDF]
    extends FormModule[Rdf#Node, Rdf#URI] {
  // def step(formSyntax: FormSyntax): FormSyntax
}

/** starting point for implementing a step in Form generation  */
trait GenericSPARQLformProcessing[Rdf <: RDF, DATASET]
    extends GenericFormProcessing[Rdf]
    with SPARQLHelpers[Rdf, DATASET] {

  private val query = """CONTRUCT{ ?S ?P ?O} WHERE GRAPH { ?GR { ?S ?P ?O} } LIMIT 22"""

  import ops._

  def stepExample(formSyntax: FormSyntax): FormSyntax = {
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