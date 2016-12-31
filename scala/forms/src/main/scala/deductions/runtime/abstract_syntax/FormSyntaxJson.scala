package deductions.runtime.abstract_syntax

import play.api.libs.json._
import org.w3.banana.RDF

trait FormSyntaxJson[Rdf <: RDF] extends FormModule[Rdf#Node, Rdf#URI] {

  def formSyntax2JSON(formSyntax: FormSyntax) : String = Json.prettyPrint(Json.toJson(formSyntax))

  implicit val nodeWrites = new Writes[Rdf#Node] {
    def writes(n: Rdf#Node) = new JsString(n.toString())
  }

  implicit val WidgetTypeWrites = new Writes[WidgetType] {
    def writes(wt: WidgetType) = new JsString(wt.toString())
  }

  implicit val entryWrites = new Writes[Entry] {
    def writes(e: Entry) = Json.obj(
      "label" -> e.label,
      "comment" -> e.comment,
      "mandatory" -> e.mandatory,
      "property" -> e.property,
      "subject" -> e.subject,
      "type_" -> e.type_,
      "value" -> e.value,
      "widgetType" -> e.widgetType,
      "openChoice" -> e.openChoice
      // , "possibleValues" -> e.possibleValues
      )
  }

  implicit val resourceEntryWrites = new Writes[ResourceEntry] {
    def writes(e: ResourceEntry) = Json.obj(
      "valueLabel" -> e.valueLabel)
  }

  implicit val formSyntaxWrites = new Writes[FormSyntax] {
    def writes(fs: FormSyntax) = Json.obj(
      "subject" -> fs.subject,
      "fields" -> fs.fields)
  }

}