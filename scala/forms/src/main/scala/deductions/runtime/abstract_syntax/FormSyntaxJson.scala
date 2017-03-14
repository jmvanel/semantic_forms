package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

import deductions.runtime.utils.RDFHelpers
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsValue

trait FormSyntaxJson[Rdf <: RDF]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFHelpers[Rdf] {

  def formSyntax2JSONString(formSyntax: FormSyntax) : String = Json.prettyPrint(formSyntax2JSON(formSyntax))
  def formSyntax2JSON(formSyntax: FormSyntax): JsValue = Json.toJson(formSyntax)

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
    def writes(e: Entry) = {
      println(s"entryWrites: e getClass ${e.getClass()} $e")
      e match {
        case r: ResourceEntry =>
          Json.obj(
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
            "cardinality" -> e.cardinality,
            "htmlName" -> e.htmlName,
            // , "possibleValues" -> e.possibleValues
            "valueLabel" -> r.valueLabel,
            //      if (r.thumbnail != null)
            "thumbnail" -> r.thumbnail,
            "isImage" -> r.isImage)
        case r: Entry =>
          /* TODO this code is pasted from other case above */
          Json.obj(
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
            "cardinality" -> e.cardinality,
            "htmlName" -> e.htmlName // , "possibleValues" -> e.possibleValues
            )
      }
    }
  }

  /** TODO this code is not used by Play JSON lib */
  implicit val resourceEntryWrites = new Writes[ResourceEntry] {
    def writes(e: ResourceEntry) = Json.obj(
      "valueLabel" -> e.valueLabel,
      "thumbnail" -> e.thumbnail,
      "isImage" -> e.isImage
    )
  }

  implicit val formSyntaxWrites = new Writes[FormSyntax] {
    def writes(fs: FormSyntax) = Json.obj(
      "subject" -> fs.subject,
      "title" -> fs.title,
      "thumbnail" -> fs.thumbnail,
       "formURI" -> fs.formURI,
       "formLabel" -> fs.formLabel,

      "fields" -> fs.fields)
  }

}