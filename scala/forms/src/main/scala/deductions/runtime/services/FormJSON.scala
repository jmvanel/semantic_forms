package deductions.runtime.services

import deductions.runtime.abstract_syntax.{FormSyntaxFactory, FormSyntaxJson}
import org.w3.banana.RDF

/** service producing the raw form syntax in JSON */
trait FormJSON[Rdf <: RDF, DATASET]
    extends FormSyntaxFactory[Rdf, DATASET]
    with FormSyntaxJson[Rdf] {

  import ops._

  def formData(uri: String, blankNode: String = "", Edit: String = "", formuri: String = "",
      database: String = "TDB", lang: String="en"): String = {
    formAsJSON(URI(uri), Edit != "", formuri, database, lang)
  }

  private def formAsJSON(subject: Rdf#Node, editable: Boolean, formuri: String = "",
      database: String, lang0: String="en"): String = {
    val datasetOrDefault = getDatasetOrDefault(database)
//    val formSyntax = rdfStore.rw(
//      datasetOrDefault, {

    		implicit val graph = allNamedGraph
    		implicit val lang: String=lang0
//        createForm(subject, editable, formuri = formuri)
//      })
//      .get
    
    		// TODO datasetOrDefault
    val formSyntax = createFormTR(subject, editable, formuri = formuri)
    formSyntax2JSONString(formSyntax)
  }
}