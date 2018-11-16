package deductions.runtime.html

import deductions.runtime.core.FormModule
import deductions.runtime.utils.{RDFPrefixesInterface, UnicityList}

import scala.collection.mutable
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest
import scala.xml.Unparsed

/** Table View; editable or not */
trait TableView[NODE, URI <: NODE]
    //extends Form2HTMLBase[NODE, URI]
    extends Form2HTML[NODE, URI]
    with HTML5Types
    with FormModule[NODE, URI]
    with RDFPrefixesInterface {

  //  type formMod = FormModule[NODE, URI]
  type FormSyntax = formMod#FormSyntax
  type Entry = formMod#Entry

  private val properties = UnicityList[NODE]
  private val rows = UnicityList[NODE]
  private val cellsMap = mutable.Map[(NODE, NODE), List[Entry]]().withDefaultValue(List())
  /** used for generating special URI (first) column */
  private val rowsMap = mutable.Map[NODE, Entry]()
  /** used for printing property label in header */
  private val propertiesMap = mutable.Map[NODE, Entry]()

  /** inspired by https://stackoverflow.com/questions/7558182/sort-a-table-fast-by-its-first-column-with-javascript-or-jquery */
  private val sortingJavascript = """
    if (typeof(asc) == "undefined") 
      var asc = 1

    function sortTable(colSortIndex){
    console.log("Sorting on column " + colSortIndex)
    var tbl = document.getElementById("sf-table").tBodies[0];
//    console.log("table tBodies") ; console.log(tbl)
    var store = [];
    for(var i=1, len=tbl.rows.length; i<len; i++){
        var row = tbl.rows[i];
//        console.log("row") ; console.log(row)
        var sortnr = (row.cells[colSortIndex].textContent || row.cells[colSortIndex].innerText);
        store.push([sortnr, row]);
//        console.log("sortnr") ; console.log(sortnr)
    }
    store.sort(
      function(x,y){
        return (x[0] == y[0]) ? 0 :
              ((x[0] >  y[0]) ? asc : -1 * asc);
    })
    for(var i=0, len=store.length; i<len; i++){
        tbl.appendChild(store[i][1]);
    }
    store = null;
    asc = -asc
}"""

  /** generate HTML Table View from triples in FormSyntax */
  def generate(form: formMod#FormSyntax, request: HTTPrequest): NodeSeq = {

    for (entry <- form.fields) {
      properties.add(entry.property)
      propertiesMap(entry.property) = entry
    }
    for (entry <- form.fields) {
      rows.add(entry.subject)
      val key = (entry.subject, entry.property)
      cellsMap(key) = entry :: cellsMap(key)
      rowsMap(entry.subject) = entry
    }
    logger.info( s"TableView.generate: cells count: ${cellsMap.size}")

    <p>{
      request.getHTTPparameterValue("label").getOrElse("")
    }</p> ++
    <script>{Unparsed(sortingJavascript)}</script> ++
    <table class="table table-striped table-bordered" id="sf-table">
      <tr> { headerRow } </tr>
      {
        for (row <- rows.list) yield {
          <tr>
            <td> { uriColumn(row) } </td>
            { dataColumns(row, request) }
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
        <th onclick="sortTable(this.cellIndex)"
            title="Click to sort">{
          val entry = propertiesMap(property)
          makeFieldLabel(NullResourceEntry, entry, editable = false)(nullFormSyntax)
        }</th>
      }
    })

  /** URI column */
  private def uriColumn(row: NODE): NodeSeq = {
    val entry = rowsMap(row)
    hyperlinkToURI(config.hrefDisplayPrefix, entry.subject.toString(),
      entry.subjectLabel,
      firstNODEOrElseEmptyString(entry.type_),
      NullResourceEntry) ++
    hyperlinkForEditingURI(toPlainString(row), "en")
  }

  /** data (triple objects) columns */
  private def dataColumns(row: NODE, request: HTTPrequest) = {
    for (property <- properties.list) yield {
      Seq(
        <td>{
          val cellOption = cellsMap.get((row, property))
          cellOption match {
            case Some(entriesList) =>
              for( entry <- entriesList) yield {
//              println(s"dataColumns: isEditableFromRequest ${isEditableFromRequest(request)}, entry $entry")
                createHTMLField(entry,
                  editable = isEditableFromRequest(request),
                  displayInTable = true, request=request )(nullFormSyntax)
              }
            case _ => ""
          }
        }</td>)
   }
  }

  /** TODO also elsewhere */
  private def isEditableFromRequest(request: HTTPrequest): Boolean =
    request.queryString.getOrElse("edit", Seq()).headOption.getOrElse("") != ""

}
