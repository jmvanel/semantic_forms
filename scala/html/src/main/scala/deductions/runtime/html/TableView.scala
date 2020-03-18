package deductions.runtime.html

import deductions.runtime.core.FormModule
import deductions.runtime.utils.{RDFPrefixesInterface, UnicityList}

import scala.collection.mutable
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest
import scala.xml.Unparsed
import deductions.runtime.utils.I18NMessages
import scala.xml.Text
import scala.xml.Comment

/** Table View; editable or not; usable one time, table data structures are never cleaned */
trait TableView[NODE, URI <: NODE]
    extends Form2HTML[NODE, URI]
    with HTML5Types
    with FormModule[NODE, URI]
    with RDFPrefixesInterface {

  //  type formMod = FormModule[NODE, URI]
  type FormSyntax = formMod#FormSyntax
  type Entry = formMod#Entry

  private val properties = UnicityList[NODE]()
  private var rows = UnicityList[NODE]()
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

  /** generate HTML Table View (or paragraphs=sentences) from triples in FormSyntax */
  def generate(form: formMod#FormSyntax, request: HTTPrequest): NodeSeq = {
    populateTableStructures(form)
    val orderby = request.getHTTPparameterValue("orderby").getOrElse("")
    if(orderby != "") {
      orderBy(stringToAbstractURI(orderby))
      println(s"Sorted by $orderby - <${stringToAbstractURI(orderby)}>")
    }
    val paragraphs = request.getHTTPparameterValue("paragraphs").getOrElse("")
    if( paragraphs == "on" )
      generateSummarySentencesHTML(request)
    else
      generateTableHTML(request)
  }

    /** generate HTML summary sentence of URI's à la Google search (#200) from triples in FormSyntax */
  private def generateSummarySentences(form: formMod#FormSyntax, request: HTTPrequest): NodeSeq = {
    populateTableStructures(form)
    generateSummarySentencesHTML(request)
  }

  private def populateTableStructures(form: deductions.runtime.core.FormModule[NODE,URI]#FormSyntax) = {
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
  }

  private def generateTableHTML(request: HTTPrequest): NodeSeq = {
    makeHTMLselfLink(request) ++
    <script>{Unparsed(sortingJavascript)}</script> ++
    <table class="table table-striped table-bordered" id="sf-table">
      <tr> { headerRow(request) } </tr>
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

  private def makeHTMLselfLink(request: HTTPrequest): NodeSeq =
    <p>
      <a href={request.uri}
         title="Link to this page, suitable to paste somewhere else.">{
        request.getHTTPparameterValue("label").getOrElse("")
      }</a>
    </p>

  private def generateSummarySentencesHTML(request: HTTPrequest): NodeSeq = {
    makeHTMLselfLink(request) ++
    <p class="sf-values-group" id="sf-sentences">
        {
          for (row <- rows.list) yield {
            Text("\n") ++
            <p class="sf-sentence">
              <span class="sf-value-block"> { uriColumn(row) } </span>
              { dataSentencesForRow(row, request) }
            </p>
          } ++: Comment(s"""${rows.list.toSeq.size} items for "${rowsMap(row).subjectLabel.replaceAll("--", ",")}" """)
        }
    </p>
  }

  /** */
  private def headerRow(request: HTTPrequest) = Seq(
    <th>URI</th>,
    {
      for (property <- properties.list) yield {
        <th onclick="sortTable(this.cellIndex)"
            title={ I18NMessages.get("Click_to_sort", request.getLanguage()) }>{
          val entry = propertiesMap(property)
          makeFieldLabel(NullResourceEntry, entry, editable = false)(nullFormSyntax)
        }</th>
      }
    })

  /** URI column */
  private def uriColumn(row: NODE): NodeSeq = {
    val entry = rowsMap(row)
    makeHyperlinkToURI(
      config.hrefDisplayPrefix,
      entry.subject.toString(),
      entry.subjectLabel, Seq()) ++
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

  /** data (triple objects) <span> elements */
  private def dataSentencesForRow(row: NODE, request: HTTPrequest): NodeSeq = {
    val x = for (property <- properties.list.toSeq) yield {
      val cellOption = cellsMap.get((row, property))
      val xx = cellOption match {
        case Some(entriesList) =>
          for (entry <- entriesList) yield {
            // println(s"dataColumns: isEditableFromRequest ${isEditableFromRequest(request)}, entry $entry")
            Comment({entry.label}) ++
            {
              createHTMLField(
                entry,
                editable = isEditableFromRequest(request),
                displayInTable = true, request = request)(nullFormSyntax)
            } ++
            Text(", ")
          }
        case _ => NodeSeq.Empty
      }
      xx . flatten ++ (if (xx.size >1) <span> ; </span> else NodeSeq.Empty)
    }
    x .flatten
  }

  /** TODO also elsewhere */
  private def isEditableFromRequest(request: HTTPrequest): Boolean =
    request.queryString.getOrElse("edit", Seq()).headOption.getOrElse("") != ""

  private def orderBy(property: NODE) = {
    def comparison(n: NODE) = cellsMap(n, property).headOption.getOrElse(NullResourceEntry).valueLabel
    val l = rows.list.toSeq.sortWith { (n1, n2) =>
      comparison(n1) < comparison(n2) }
//    println(s"orderBy $property : l $l")
    rows = UnicityList(l)
//    println(s"orderBy $property : rows ${rows.list}")
  }
}
