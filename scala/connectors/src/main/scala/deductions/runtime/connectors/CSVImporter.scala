package deductions.runtime.connectors

import java.io.{InputStream, InputStreamReader}
import java.lang.Character.toUpperCase
import java.util.StringTokenizer

import org.apache.any23.vocab.CSV
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
//import org.apache.commons.csv.Constants

import deductions.runtime.utils.{Configuration, RDFPrefixes, URIHelpers}
import org.w3.banana.{RDF, RDFOps, RDFPrefix, XSDPrefix}

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/** made from CSVExtractor from Any23;
 *  TODO: probably should be in another SBT project */
trait CSVImporter[Rdf <: RDF, DATASET]
		extends URIHelpers
		with RDFPrefixes[Rdf]
    with CSVmappings[Rdf] {

  val config: Configuration

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  private val rdf = RDFPrefix[Rdf]
//  private val rdfs = RDFSPrefix[Rdf]
  private val xsd = XSDPrefix[Rdf]

//  val Constants = new org.apache.commons.csv.Constants()
  val TAB = '\t'

  ///////////////////////////////////
  
  private var csvParser: CSVParser = _
  type URI = Rdf#URI

  private var headerURIs: IndexedSeq[URI] = _

  private val csv: CSV = CSV.getInstance

  /** TODO consider using R2RML vocab' */
  private def csvPredicate(p: String) = URI(CSV.NS + p)

  /** run the parser and "semantize"
   *  @param uriPrefix	URI prefix to be predended to column names */
  def run(
      in: InputStream,
      uriPrefix: URI,
      /* property Value pair to add For Each Row */
      propertyValueForEachRow: List[(Rdf#URI, Rdf#Node)] = List(),
      separator: Char = ','): Rdf#Graph = {

    val rowType = csvPredicate(CSV.ROW_TYPE)
    val csvFormat = CSVFormat.DEFAULT.withDelimiter(separator).withHeader()

    csvParser = new CSVParser( new InputStreamReader(in), csvFormat)
    val header: java.util.Map[String, Integer] = csvParser.getHeaderMap
    headerURIs = processHeader(header, uriPrefix)
    
    val list = ArrayBuffer[Rdf#Triple]()
    
    writeHeaderPropertiesMetadata(header, list)
    val rowSubjectPrefix = {
      val doc = uriPrefix.toString
      if( doc.endsWith("/") ||
        doc.endsWith("#") ) doc + "row/"
      else
        doc + "/row/"
    }
    println( s"rowSubjectPrefix: $rowSubjectPrefix")

    var rowIndex = 0
    for( record <- csvParser.getRecords.asScala ) {
      val rowSubject = makeRowSubject(rowSubjectPrefix, rowIndex, record)
      produceRowStatements(rowSubject, record, list, uriPrefix )
      for( pv <- propertyValueForEachRow ) {
    	  list += Triple(rowSubject, pv._1, pv._2)
      }
      // list += Triple(rowSubject, csvPredicate(CSV.ROW_POSITION), Literal( String.valueOf(index) ) )
      rowIndex = rowIndex + 1
    }
    addTableMetadataStatements(uriPrefix, list, rowIndex, headerURIs.length)
    makeGraph(list)
  }

  private def findIDcolumnIndex(uriPrefix: URI) : Int = {
    headerURIs
      .indexWhere(
        uri =>
          isIDcolumn(fromUri(uri), uriPrefix))
  }

  private def makeRowSubject(rowSubjectPrefix: String, index: Int, record: CSVRecord) = {
    val values = record.iterator()
    // println( s"rowSubject : $rowSubject")
    for (cell <- values.asScala) {
    }
    URI(rowSubjectPrefix + index)
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

  private def writeHeaderPropertiesMetadata(
    header: java.util.Map[String, Integer],
    list: ArrayBuffer[Rdf#Triple]) {
    var index = 0
    for (singleHeader <- headerURIs) {
      if (index <= headerURIs.length) {
        if (!isAbsoluteURI(fromUri(singleHeader))) {
          list += Triple(singleHeader, rdfs.label, Literal(fromUri(singleHeader)))
        }
        list += Triple(singleHeader, csvPredicate(CSV.COLUMN_POSITION), Literal(String.valueOf(index), xsd.integer))
        index = index + 1
      }
    }
  }

  private def processHeader(header:  java.util.Map[String, Integer],
      documentURI: URI): ArrayBuffer[URI] = {
    val result = ArrayBuffer.fill( header.size )(URI(""))
    var index = 0
    for (h <- (header.keySet).asScala) {
      val candidate = h.trim()
      val headerURI =
        recognizePrefixedURI(candidate) match {
        case Some(uri) => uri
        case None => manageColumnsMapping(candidate, documentURI)
      }
      result . update( index, headerURI )
      index += 1
    }
    result
  }

  /** Take prefixed URI's like foaf:name, and expand them */
  def recognizePrefixedURI(candidate: String): Option[Rdf#URI] = {
    if (isAbsoluteURI(candidate) ||
        candidate.startsWith(":") ) {
      // accept prefixed URI's like foaf:name, and expand them
      val expcan = expand(candidate)
      // println(s"candidate $candidate, expand $expcan")
      expcan match {
        case Some(uri) => Some(uri)
        case None      => Some(URI(candidate))
      }
    } else None
  }


  /** manage Mapping from Column name to URI */
  private def manageColumnsMapping(columnName: String, documentURI: URI): URI = {
    println(s"manageColumnsMapping: columnName '$columnName': <${columnsMappings.getOrElse( columnName, normalize( columnName, documentURI) )}>")
    columnsMappings.getOrElse( columnName.trim(),
        normalize( columnName, documentURI) )
  }

  /** normalize column names: remove "&", "?", "/", capitalize;
   * prepend document URI */
  private def normalize(toBeNormalized0: String, documentURI: URI): URI = {
    val toBeNormalized = toBeNormalized0.trim().toLowerCase().
    replace("?", "").
    replace("/", "").
    replace("&", "")
    val result = new StringBuilder(documentURI.toString)
    val tokenizer = new StringTokenizer(toBeNormalized, " ")
    while (tokenizer.hasMoreTokens()) {
      val current = tokenizer.nextToken()
      result.append(toUpperCase(current.charAt(0))).append(current.substring(1))
    }
    URI(result.toString)
  }

  private def isIDcolumn(columnURI: String, documentURI: URI): Boolean = {
    columnURI == documentURI + "Id"
  }

  private def produceRowStatements(
    rowSubject: URI,
    record:     CSVRecord,
    list:       ArrayBuffer[Rdf#Triple],
    uriPrefix:  URI) {
    val idColumnIndex = findIDcolumnIndex(uriPrefix)
    val rowSubjectOrID : Rdf#URI = if(idColumnIndex >= 0 ) {
//      println(s"rowSubjectOrID uriPrefix $uriPrefix, ${record.get(idColumnIndex)} ")
      URI(new java.net.URI(fromUri(uriPrefix)).resolve(record.get(idColumnIndex)).toString())
//      ops.resolve(uriPrefix, URI(record.get(idColumnIndex)))
    } else
      rowSubject
    val values = record.iterator()
    var index = 0
    // println( s"rowSubjectOrID : $rowSubjectOrID")
    for (cell <- values.asScala) {
      if (index < headerURIs.length) {
        val predicate = headerURIs(index)
        if (cell =/= "") {
//          if (isIDcolumn(fromUri(predicate), uriPrefix)) {
//          } else {
            val `object` = getObjectFromCell(cell)
            // println(s"produceRowStatements: Triple : ${Triple(rowSubjectOrID, predicate, `object`)}")
            val simpleCaseTriple = Triple(rowSubjectOrID, predicate, `object`)
            val (isComplexCase, complexCaseList) = splitPropertyChain(simpleCaseTriple)
            list ++= (if (isComplexCase) complexCaseList else List(simpleCaseTriple))
//          }
        }
        index += 1
      }
    }
  }

  /** create intermediate blank node from Property Chain in column header; eg
   *  Transform:
   *  <subject>
   *    <http://purl.org/dc/terms/rights/cc:attributionName>
          "Frédéric Urien" ;
   *  into
   *  <subject> <http://purl.org/dc/terms/rights/> _:n1 .
   *  _:n1 cc:attributionName "Frédéric Urien" .
   */
  private def splitPropertyChain(tr: Rdf#Triple): (Boolean, List[Rdf#Triple]) = {
    def uriLastPathElementIsTurtleAbbreviated(p: Rdf#URI): (Boolean, Rdf#URI, Rdf#URI) = {
//      println(s"uriLastPathElementIsTurtleAbbreviated(p:$p")
      val path = new java.net.URI(fromUri(p)).getPath
//      println(s"path $path")
      if( path == null )
        return ((false, p, URI("")))
      val pathElements = path.split("/")
      val lastPathElement = pathElements .last
//      println(s"lastPathElement $lastPathElement")
      recognizePrefixedURI(lastPathElement) match {
        case Some(uri) =>
          (true, URI(fromUri(p).replace("/" + lastPathElement, "")), URI(expandOrUnchanged(fromUri(uri))))
        case None => (false, p, URI(""))
      }
    }
    val (isTurtleAbbreviated, p1, p2) = uriLastPathElementIsTurtleAbbreviated(tr.predicate)
    if (isTurtleAbbreviated) {
      val blank = createBlankNode(tr.subject, p1)
      (true, List(
        Triple(tr.subject, p1, blank),
        Triple(blank, p2, tr.objectt)))
    } else
      (false, List())
  }

  private var currentBlankNode = 0
  private var currentSubject : Rdf#Node = URI("")
  private var currentProperty = URI("")
  private def createBlankNode(row: Rdf#Node, property: Rdf#URI) = {
//    println(s">>>> createBlankNode <$row>, <$property>")
    if( row != currentSubject ||
        property != currentProperty ) {
      currentBlankNode = currentBlankNode + 1
      currentSubject = row
      currentProperty = property
    }
    val ret = s"n$currentBlankNode"
    BNode(ret)
  }

  /** get RDF triple Object From Cell */
  private def getObjectFromCell(cell0: String): Rdf#Node = {
    val cell = cell0.trim()
    // println(s"cell '$cell'  isAbsoluteURI(cell) ${isAbsoluteURI(cell)}")
    if (isAbsoluteURI(cell)) {
        URI(expandOrUnchanged(cell))

    } else if( cell.contains(":")) {
    	recognizePrefixedURI(cell) match {
        case Some(uri) => uri
        case None => Literal(cell)
      }
      
    } else {

      val datatype =
        if (isInteger(cell)) {
          xsd.integer
        } else if (isFloat(cell)) {
          xsd.float
        } else
          xsd.string        
      Literal(cell, datatype)
    }
  }
  
  private def addTableMetadataStatements(documentURI: URI, 
		  list: ArrayBuffer[Rdf#Triple],
		  numberOfRows: Int, 
      numberOfColumns: Int) {
    list += Triple(documentURI, csvPredicate(CSV.NUMBER_OF_ROWS), Literal(String.valueOf(numberOfRows), xsd.integer))
    list += Triple(documentURI, csvPredicate(CSV.NUMBER_OF_COLUMNS), Literal(String.valueOf(numberOfColumns), 
      xsd.integer))
  }

}
