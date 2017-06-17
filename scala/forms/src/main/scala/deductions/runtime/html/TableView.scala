package deductions.runtime.html

import scala.xml.NodeSeq
import scala.collection.mutable

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.UnicityList

/** Table View; base for a future editable Table View */
trait TableView[NODE, URI <: NODE]
extends Form2HTMLBase[NODE, URI]
with FormModule[NODE, URI] {

//  type formMod = FormModule[NODE, URI]
  type FormSyntax = formMod#FormSyntax
  type Entry = formMod#Entry

  private val properties = UnicityList[NODE]
  private val rows = UnicityList[NODE]
  private val m = mutable.Map[(NODE, NODE), Entry]()
  /** used for printing property label in header */
  private val propertiesMap = mutable.Map[NODE, Entry]()

  def generate(form: formMod#FormSyntax): NodeSeq = {

    for (entry <- form.fields) {
      properties.add(entry.property)
      propertiesMap(entry.property) = entry
    }
    for (entry <- form.fields) {
      rows.add(entry.subject)
      m((entry.subject, entry.property)) = entry
    }

    <table>
      <tr>{
        for (property <- properties.list) yield {
          <th>{
            val entry = propertiesMap(property)
            makeFieldLabel(NullResourceEntry, entry, editable = false)(nullFormSyntax)
            //            propertiesMap(property).label
          }</th>
        }
      }</tr>
{
      for (row <- rows.list) yield {
        <tr>{
          for (property <- properties.list) yield {
            <td>{
              val cellOption = m.get((row, property))
              cellOption match {
                case Some(entry) => entry.value
                case _           => ""
              }
            }</td>
          }
        }</tr>
      }
}
    </table>
  }
}
