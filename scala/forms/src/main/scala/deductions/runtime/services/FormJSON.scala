package deductions.runtime.services

import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.FormSyntaxJson
import deductions.runtime.abstract_syntax.FormSyntaxFactory
import play.api.libs.json._

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
    val formSyntax = rdfStore.rw(
      datasetOrDefault, {
        implicit val graph = allNamedGraph
        implicit val lang: String=lang0
        createForm(subject, editable, formuri = formuri)
      })
      .get
      formSyntax2JSONString(formSyntax)

  }
}