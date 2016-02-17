package deductions.runtime.utils

import java.lang.Character.toUpperCase
import org.apache.commons.csv.CSVParser
import java.io.IOException
import java.io.InputStream
import java.util.StringTokenizer
import org.w3.banana.RDF
import org.w3.banana._
import scala.collection.JavaConversions._
import org.apache.any23.extractor.csv.CSVReaderBuilder
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import org.apache.commons.csv.CSVRecord
import org.apache.any23.vocab.CSV

/** made from CSVExtractor from Any23;
 *  TODO: probably should be in another SBT project */
trait CSVImporter[Rdf <: RDF, DATASET] {

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  val rdf = RDFPrefix[Rdf]
  val rdfs = RDFSPrefix[Rdf]
  val xsd = XSDPrefix[Rdf]
  
  private var csvParser: CSVParser = _
  type URI = Rdf#URI

  private var headerURIs: IndexedSeq[URI] = _

  private var csv: CSV = CSV.getInstance

  def setStopAtFirstError(f: Boolean) {  }

  def run(
      in: InputStream,
      documentURI: URI ): Rdf#Graph = {
//    val documentURI: URI = // extractionContext.getDocumentURI
    
    val rowType = URI(CSV.ROW_TYPE)
    
    csvParser = CSVReaderBuilder.build(in)
    val header: java.util.Map[String, Integer] = csvParser.getHeaderMap // getLine
    headerURIs = processHeader(header, documentURI)
    
    val list = ArrayBuffer[Rdf#Triple]()
    
    writeHeaderPropertiesMetadata(header, list)
//    var nextLine: Array[String] = null
    var index = 0
    for( record <- csvParser.getRecords ) {
      val rowSubject = URI(documentURI.toString + "/row/" + index)
      list += Triple(rowSubject, rdf.typ, rowType)
      produceRowStatements(rowSubject, record, list)
//      list += Triple(documentURI, csv.row, rowSubject)
//      list += Triple(rowSubject, csv.rowPosition, new LiteralImpl(String.valueOf(index))
      index = index + 1
    }
    addTableMetadataStatements(documentURI, list, index, headerURIs.length)
    makeGraph(list)
  }

  private def isInteger(number: String): Boolean = {
    try {
      java.lang.Integer.valueOf(number)
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  private def isFloat(number: String): Boolean = {
    try {
      java.lang.Float.valueOf(number)
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  private def writeHeaderPropertiesMetadata(header: java.util.Map[String, Integer],
//      Array[String], 
      list: ArrayBuffer[Rdf#Triple]
      ) {
    var index = 0
    for (singleHeader <- headerURIs) {
      if (index > headerURIs.length) {
        //break
      }
      if (!isAbsoluteURI(fromUri(singleHeader))) {
        list += Triple( singleHeader, rdfs.label, Literal( fromUri(singleHeader) ) )
      }
      list += Triple(singleHeader, URI(CSV.COLUMN_POSITION), Literal(String.valueOf(index), xsd.integer ))
      index = index + 1
    }
  }

  private def processHeader(header:  java.util.Map[String, Integer],
      documentURI: URI): IndexedSeq[URI] = {
    val result = ArrayBuffer[URI]()
    var index = 0
    for (h <- header.keys) {
      val candidate = h.trim()
      result(index) = if (isAbsoluteURI(candidate)) URI(candidate) else normalize(candidate, 
        documentURI)
      index += 1
    }
    result
  }

  private def normalize(toBeNormalized0: String, documentURI: URI): URI = {
    val toBeNormalized = toBeNormalized0.trim().toLowerCase().replace("?", "")
      .replace("&", "")
    val result = new StringBuilder(documentURI.toString)
    val tokenizer = new StringTokenizer(toBeNormalized, " ")
    while (tokenizer.hasMoreTokens()) {
      val current = tokenizer.nextToken()
      result.append(toUpperCase(current.charAt(0))).append(current.substring(1))
    }
    URI(result.toString)
  }

  private def produceRowStatements(
    rowSubject: URI,
    record: CSVRecord,
    list: ArrayBuffer[Rdf#Triple]) {
    val values = record.iterator()
    var index = 0
    for (cell <- values) {
      if (index < headerURIs.length) {
        if (cell != "") {
          val predicate = headerURIs(index)
          val `object` = getObjectFromCell(cell)
          list += Triple(rowSubject, predicate, `object`)
        }
        index += 1
      }
    }
  }

  private def getObjectFromCell(cell0: String): Rdf#Node = {
    var `object`: Rdf#Node = Literal("")
    val cell = cell0.trim()
    if (isAbsoluteURI(cell)) {
      `object` = URI(cell)

    } else {

      var datatype =
        if (isInteger(cell)) {
          xsd.integer
        } else if (isFloat(cell)) {
          xsd.float
        } else
          xsd.string        

      `object` = Literal(cell, datatype)
    }
    `object`
  }

  private def addTableMetadataStatements(documentURI: URI, 
		  list: ArrayBuffer[Rdf#Triple],
		  numberOfRows: Int, 
      numberOfColumns: Int) {
    list += Triple(documentURI, URI(CSV.NUMBER_OF_ROWS), Literal(String.valueOf(numberOfRows), xsd.integer))
    list += Triple(documentURI, URI(CSV.NUMBER_OF_COLUMNS), Literal(String.valueOf(numberOfColumns), 
      xsd.integer))
  }
  
  def isAbsoluteURI(uri: String) = {
    try{
      val u = new java.net.URI(uri)
      u.isAbsolute()
    } catch {
      case t: Throwable => false
    }
  }

}
