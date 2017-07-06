package deductions.runtime.html

import deductions.runtime.abstract_syntax.FormModule
import deductions.runtime.utils.UnicityList

import scala.collection.mutable
import scala.xml.NodeSeq

/** Table View; base for a future editable Table View */
trait TableView[NODE, URI <: NODE]
    //extends Form2HTMLBase[NODE, URI]
    extends Form2HTML[NODE, URI]
    with HTML5Types
    with FormModule[NODE, URI] {

  //  type formMod = FormModule[NODE, URI]
  type FormSyntax = formMod#FormSyntax
  type Entry = formMod#Entry

  private val properties = UnicityList[NODE]
  private val rows = UnicityList[NODE]
  private val cellsMap = mutable.Map[(NODE, NODE), Entry]()
  private val rowsMap = mutable.Map[NODE, Entry]()
  /** used for printing property label in header */
  private val propertiesMap = mutable.Map[NODE, Entry]()

  def generate(form: formMod#FormSyntax): NodeSeq = {

    for (entry <- form.fields) {
      properties.add(entry.property)
      propertiesMap(entry.property) = entry
    }
    for (entry <- form.fields) {
      rows.add(entry.subject)
      cellsMap((entry.subject, entry.property)) = entry
      rowsMap(entry.subject) = entry
    }

    <table class="table table-striped table-bordered">
      <tr> { headerRow } </tr>
      {
        for (row <- rows.list) yield {
          <tr>
            <td> { uriColumn(row) } </td>
            { dataColumns(row) }
          </tr>
        }
      }
    </table>
  }

  /** */
  private def headerRow = Seq(
    <th>URI</th>,
    {
      for (property <- properties.list) yield {
        <th>{
          val entry = propertiesMap(property)
          makeFieldLabel(NullResourceEntry, entry, editable = false)(nullFormSyntax)
        }</th>
      }
    })

  /** URI column */
  private def uriColumn(row: NODE) = {
    val entry = rowsMap(row)
    hyperlinkToURI(config.hrefDisplayPrefix, entry.subject.toString(),
      entry.subjectLabel,
      entry.type_,
      NullResourceEntry)
  }

  /** data (triple objects) columns */
  private def dataColumns(row: NODE) = {
    for (property <- properties.list) yield {
      Seq(
        <td>{
          val cellOption = cellsMap.get((row, property))
          cellOption match {
            case Some(entry) =>
              createHTMLField(entry, editable = false, displayInTable = true)(nullFormSyntax)
            case _ => ""
          }
        }</td>)
    }
  }
      
}
