package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/** create properties Groups */
trait FormGroups[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>
  
  val expandPropertiesGroups = (graph: Rdf#Graph, lang: String) => (formSyntax: FormSyntax) => {

    // create the form entries from groups 
    val entriesFromPropertiesGroups = for ((node, formSyntax) <- formSyntax.propertiesGroupMap)
      yield node -> makeEntriesFromFormSyntax(formSyntax)(graph)

    // set a FormSyntax.title for each group in propertiesGroups
    val propertiesGroups = for ((node, entries) <- entriesFromPropertiesGroups) yield {
      FormSyntax(node, entries, title = makeInstanceLabel(node, allNamedGraph, lang))
    }
    formSyntax.copy(propertiesGroups = propertiesGroups.toSeq)
  }

}