package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix

/**
 * Automatically show inferred values from
 *  owl:sameAs axioms
 */
trait OWLsameAsFormProcessing[Rdf <: RDF, DATASET]
    extends GenericSPARQLformProcessing[Rdf, DATASET] {
  import ops._
  private val owl = OWLPrefix[Rdf]

  private val query: String =
    s"""|${declarePrefix(owl)}
       |CONSTRUCT{
       | ?O ?PP ?OO .
       | ?S ?PPP ?OOO .
       |}
       |WHERE {
       | {
       |   GRAPH ?GR_ORIG {
       |     <subject> owl:sameAs ?O .
       |   }
       |   GRAPH ?GR_SAMEAS {
       |      ?O ?PP ?OO .
       |   }
       | } UNION {
       |   # TODO GRAPH ?GR_ORIG2 ... ?GR_SAMEAS2
       |      ?S owl:sameAs <subject> .
       |      ?S ?PPP ?OOO .
       | }
       | FILTER ( ?GR_SAMEAS != <subject> )
       |}
       |""".stripMargin

  def addOWLsameAs(formSyntax: FormSyntax): FormSyntax = {
    val subject = "<" + nodeToString(formSyntax.subject) + ">"
    val query2 = query.replaceAll("<subject>", subject)

    for (
//      graph <- sparqlConstructQueryGraph(query2);
        graph <-  sparqlConstructQuery(query2);
      _ = println(s"OWLsameAsFormProcessing: After sparqlConstructQueryGraph graph size ${graph.size}") ;
      tr <- getTriples(graph)
    ) {
    	println(s"OWLsameAsFormProcessing: loop ${tr}") ;
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