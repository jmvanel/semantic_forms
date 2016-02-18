package deductions.runtime.utils

import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Character.toUpperCase
import java.util.StringTokenizer
import scala.annotation.migration
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import org.apache.any23.vocab.CSV
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.XSDPrefix
import org.w3.banana.FOAFPrefix
import org.w3.banana.Prefix
import org.w3.banana.PrefixBuilder
import deductions.runtime.services.DefaultConfiguration

/** made from CSVExtractor from Any23;
 *  TODO: probably should be in another SBT project */
trait CSVImporter[Rdf <: RDF, DATASET]
		extends DefaultConfiguration {

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  private val rdf = RDFPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]
  private val xsd = XSDPrefix[Rdf]
  private val foaf = FOAFPrefix[Rdf]

  /** lots of boiler plate for vocabularies !!!!!!!!!!! */
  object CCOPrefix {
    def apply[Rdf <: RDF: RDFOps](implicit ops: RDFOps[Rdf]) = new CCOPrefix(ops)
  }
  class CCOPrefix[Rdf <: RDF](ops: RDFOps[Rdf])
      extends PrefixBuilder("cco", "http://purl.org/ontology/cco/core#")(ops) {
    val expertise = apply("expertise")
  }
  private val cco = CCOPrefix[Rdf]
  
  object AVPrefix {
    def apply[Rdf <: RDF: RDFOps](implicit ops: RDFOps[Rdf]) = new AVPrefix(ops)
  }
  class AVPrefix[Rdf <: RDF](ops: RDFOps[Rdf])
      extends PrefixBuilder("av", prefixAVontology )(ops) {
      val idea = apply("idea")
      val contributesToOrganization = apply("contributesToOrganization")
      val metAt = apply("metAt")
  }
  private val av = AVPrefix[Rdf]

  object VCardPrefix {
    def apply[Rdf <: RDF: RDFOps](implicit ops: RDFOps[Rdf]) = new VCardPrefix(ops)
  }
  class VCardPrefix[Rdf <: RDF](ops: RDFOps[Rdf])
      extends PrefixBuilder("vcard", "http://www.w3.org/2006/vcard/ns#" )(ops) {
	  val postal_code = apply("postal-code")
	  val locality =  apply("locality")
  }
  private val vcard = VCardPrefix[Rdf]

  ///////////////////////////////////
  
  private var csvParser: CSVParser = _
  type URI = Rdf#URI

  private var headerURIs: IndexedSeq[URI] = _

  private var csv: CSV = CSV.getInstance

  /** TODO consider using R2RML vocab' */
  private def csvPredicate(p: String) = URI(CSV.NS + p)
    
  def run(
      in: InputStream,
      documentURI: URI,
      typeURI: URI = foaf.Person ): Rdf#Graph = {
    
    val rowType = csvPredicate(CSV.ROW_TYPE)
    
    csvParser = new CSVParser( new InputStreamReader(in) , CSVFormat.DEFAULT .withHeader() )
      // public CSVParser(final Reader reader, final CSVFormat format) throws IOException {
      // CSVParser.create(in) // 
      // CSVReaderBuilder.build(in)
    val header: java.util.Map[String, Integer] = csvParser.getHeaderMap
    headerURIs = processHeader(header, documentURI)
    
    val list = ArrayBuffer[Rdf#Triple]()
    
    writeHeaderPropertiesMetadata(header, list)
    var index = 0
    val rowSubjectPrefix = {
      val doc = documentURI.toString
      if( doc.endsWith("/") ||
        doc.endsWith("#") ) doc + "row/"
      else
        doc + "/row/"
    }
    for( record <- csvParser.getRecords ) {
      val rowSubject = URI( rowSubjectPrefix + index)
      list += Triple(rowSubject, rdf.typ, rowType)
      produceRowStatements(rowSubject, record, list)
      list += Triple(rowSubject, rdf.typ, typeURI )
      list += Triple(documentURI, csvPredicate(CSV.ROW), rowSubject)
      list += Triple(rowSubject, csvPredicate(CSV.ROW_POSITION), Literal( String.valueOf(index) ) )
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
    for (h <- header.keys) {
      val candidate = h.trim()
      result . update( index, 
        if (isAbsoluteURI(candidate))
          URI(candidate)
        else
          manageColumnsMapping(candidate, documentURI)
      )
      index += 1
    }
    result
  }

  /** TODO
   *  use labels on properties to propose properties to user,
   *  manage prefixes globally, maybe using prefix.cc */
  val columnsMappings = Map(
      "Email" -> foaf.mbox,
      "Prénom" -> foaf.givenName,
      "Nom" -> foaf.familyName,
      "Téléphone" -> foaf.phone,
      "Catégorie" -> foaf.focus,
      "Code postal" -> vcard.postal_code,
	    "Ville" -> vcard.locality,
	    "Description courte" -> foaf.name,
	    "Description Longue" -> rdfs.comment,
	    "Site Web" -> foaf.homepage,
	    
      "Projet 1" -> foaf.currentProject,
      "Projet 2" -> foaf.currentProject,
      "Projet 3" -> foaf.currentProject,
      "compétence 1" -> cco.expertise,
      "compétence 2" -> cco.expertise,
      "autre" -> cco.expertise,
      "organisation 1" -> av.contributesToOrganization,
      "organisation 2" -> av.contributesToOrganization,
      "Idée 1" -> av.idea,
      "Idée 2" -> av.idea,
      "Rencontré à" -> av.metAt
      )
  private def manageColumnsMapping(col: String, documentURI: URI): URI = {
//    println()
    columnsMappings.getOrElse( col, normalize( col, documentURI) )
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
    list += Triple(documentURI, csvPredicate(CSV.NUMBER_OF_ROWS), Literal(String.valueOf(numberOfRows), xsd.integer))
    list += Triple(documentURI, csvPredicate(CSV.NUMBER_OF_COLUMNS), Literal(String.valueOf(numberOfColumns), 
      xsd.integer))
  }
  
  private def isAbsoluteURI(uri: String) = {
    try{
      val u = new java.net.URI(uri)
      u.isAbsolute()
    } catch {
      case t: Throwable => false
    }
  }

}
