package deductions.runtime.abstract_syntax

import play.api.libs.json._
import org.w3.banana.RDF
import deductions.runtime.utils.RDFHelpers

trait FormSyntaxJson[Rdf <: RDF]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFHelpers[Rdf] {

  def formSyntax2JSON(formSyntax: FormSyntax) : String = Json.prettyPrint(Json.toJson(formSyntax))

  implicit val nodeWrites = new Writes[Rdf#Node] {
    def writes(n: Rdf#Node) = new JsString(n.toString())
  }

  implicit val WidgetTypeWrites = new Writes[WidgetType] {
    def writes(wt: WidgetType) = new JsString(wt.toString())
  }

    implicit val cardinalityWrites = new Writes[Cardinality] {
    def writes(obj: Cardinality) = new JsString(obj.toString())
  }

  implicit val entryWrites = new Writes[Entry] {
    def writes(e: Entry) = Json.obj(
      "subject" -> e.subject,
      "subjectLabel" -> e.subjectLabel,

      "property" -> e.property,
      "label" -> e.label,
      "comment" -> e.comment,
      "mandatory" -> e.mandatory,

      "value" -> nodeToString(e.value),
      "type" -> e.type_,

      "widgetType" -> e.widgetType,
      "openChoice" -> e.openChoice,
      "cardinality" -> e.cardinality
      // , "possibleValues" -> e.possibleValues
      )
  }

  implicit val resourceEntryWrites = new Writes[ResourceEntry] {
    def writes(e: ResourceEntry) = Json.obj(
      "valueLabel" -> e.valueLabel,
      "thumbnail" -> e.thumbnail,
      "isImage" -> e.isImage )
  }

  implicit val formSyntaxWrites = new Writes[FormSyntax] {
    def writes(fs: FormSyntax) = Json.obj(
      "subject" -> fs.subject,
      "title" -> fs.title,
      "thumbnail" -> fs.thumbnail,
 
      "fields" -> fs.fields)
  }

}