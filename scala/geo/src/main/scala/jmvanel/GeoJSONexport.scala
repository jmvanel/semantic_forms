package jmvanel

import org.apache.jena.tdb.TDBFactory
import org.apache.jena.query._
import collection.JavaConverters._
import java.io.FileWriter
import java.io.File
import java.net.URL
import java.io.StringReader

import org.apache.jena.riot.JsonLDWriteContext
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.writer.JsonLDWriter
import org.apache.jena.riot.system.PrefixMapStd
import titaniumJena.Jena2Titanium

//import javax.json.Json
//import javax.json.JsonObject
// import javax.json.JsonArray
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.json.JsonArray

import com.apicatalog.rdf.impl.DefaultRdfProvider
import com.apicatalog.jsonld.JsonLd
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.RdfGraph
import com.apicatalog.jsonld.document.DocumentParser
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.document.RdfDocument
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.annotation.JsonFormat

import java.io.StringWriter
import titaniumJena.Titanium2Jena
import jakarta.json.stream.JsonGenerator
import jakarta.json.JsonWriterFactory

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.api.FromRdfApi

/** export GeoJSON (both plain GeoJSON & JSON-LD) from URI's having geographic data
 *  in given TDB database */
object GeoJSONexport extends App {
  if( args.length < 1) {
    println("Args: TDB, SPARQL string")
    System.exit(0)
  }
  // Open TDB db
  val dataset = TDBFactory.createDataset(args(0))

  // Take inspiration from this to generate geojson RDF
  // this SPARQL expects plain geo: coordinates
  // Remove LIMIT 50 , and and add your own criteria in WHERE
  val defaultSPARQL = """
  PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
  PREFIX geojson: <https://purl.org/geojson/vocab#>
  prefix xsd: <http://www.w3.org/2001/XMLSchema#>
  CONSTRUCT {

    <urn:geojson> a geojson:FeatureCollection ;
      geojson:features ?S .
   
    ?S a geojson:Feature ;
         geojson:geometry ?point .
      ?point a geojson:Point ;
               geojson:coordinates ?coordinates .
    ?coordinates # a rdf:List ;
    rdf:first ?LONstring ; rdf:rest ?rest .
    ?rest rdf:first ?LATstring ; rdf:rest rdf:nil .
    ?S geojson:properties ?properties .
    ?properties ?P ?O .
    }
  WHERE {
    GRAPH ?gr {
      ?S geo:lat ?LAT .
      ?S geo:long ?LON .
      BIND( xsd:double(STR(?LAT)) AS ?LATstring)
      BIND( xsd:double(STR(?LON)) AS ?LONstring)
      BIND(BNODE() AS ?point)
      BIND(BNODE() AS ?coordinates)
      BIND(BNODE() AS ?rest)
      BIND(BNODE() AS ?properties)
      ?S ?P ?O .
      FILTER ( ?P != geo:lat )
      FILTER ( ?P != geo:long )
    }
  } LIMIT 25
  """

  val frameOptions = {
    val frameOptions = new JsonLdOptions()
    frameOptions.setUseNativeTypes(true)
    frameOptions.setOmitGraph(true)
    frameOptions.setCompactToRelative(true)
    frameOptions.setOmitDefault(true)
    // options.setExplicit(true)
    frameOptions
  }

  // SPARQL query
  val queryString =
    if( args.length >= 2 )
      args(1)
    else defaultSPARQL

  val qexec = QueryExecutionFactory.create(queryString, dataset)
  val resultModel = qexec.execConstruct()
  
  val nsPrefixMap = resultModel.getNsPrefixMap()
  println(s"nsPrefixMap: ${nsPrefixMap.asScala.mkString("; ")}")
  // empty!!!
//  val nsPrefixMapGlobal = dataset.getUnionModel.getNsPrefixMap()
//  println(s"nsPrefixMap2: ${nsPrefixMapGlobal.asScala.mkString("; ")}")

  val datasetGraph = DatasetFactory.wrap(resultModel).asDatasetGraph()
  
  // From https://geojson.org/geojson-ld/geojson-context.jsonld
  val geojsonContext = """{
  "@context": {
    "geojson": "https://purl.org/geojson/vocab#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "Feature": "geojson:Feature",
    "FeatureCollection": "geojson:FeatureCollection",
    "GeometryCollection": "geojson:GeometryCollection",
    "LineString": "geojson:LineString",
    "MultiLineString": "geojson:MultiLineString",
    "MultiPoint": "geojson:MultiPoint",
    "MultiPolygon": "geojson:MultiPolygon",
    "Point": "geojson:Point",
    "Polygon": "geojson:Polygon",
    "bbox": {
      "@container": "@list",
      "@id": "geojson:bbox"
    },
    "coordinates": {
      "@container": "@list",
      "@id": "geojson:coordinates"
    },
    "features": {
      "@container": "@set",
      "@id": "geojson:features"
    },
    "geometry": "geojson:geometry",
    "id": "@id",
    "properties": "geojson:properties",
    "type": "@type"
  } ,
  "@type": "FeatureCollection"
}"""

  val rdfProvider = DefaultRdfProvider.INSTANCE
  val titaniumOut = rdfProvider.createDataset()
//  println(s"==== SPARQL ouput ====")
  /// RDFDataMgr.write( System.out, resultModel, RDFFormat.TURTLE_PRETTY)

  Jena2Titanium.populateDataset( resultModel.getGraph, titaniumOut)
  // printTitanium(titaniumOut) // OK !!!
  val jsonObject = rdfToJsonLD(titaniumOut, geojsonContext)

  // write JsonObject
  val fileName = "out.geojson"
  val fw = new FileWriter(fileName)
  val writerFactory = makeWriterFactory
  val jsonWriter = writerFactory.createWriter(fw)
  jsonWriter.writeObject(jsonObject)
  jsonWriter.close()
  println( s"GeoJSON $fileName written")


  /** RDF To JsonLD:
   *  1) fromRdf
   *  2) frame
   *  @arg titaniumDS: Rdf Dataset
   *  @arg context: frame context
   *  */
  def rdfToJsonLD(titaniumDS: RdfDataset, context: String): JsonObject = {
    val inputStream = new StringReader(context)
    val contextDocument = DocumentParser.parse(
        MediaType.JSON_LD,
        inputStream)

    val fromRdf: FromRdfApi =
      JsonLd.fromRdf(
        RdfDocument.of(titaniumDS) ).options(frameOptions)

//    println( "TEST intermediate result")
//    printJsonArray(fromRdf . get) // OK: blank nodes as @list

    JsonLd.frame(
      JsonDocument.of(fromRdf.get), contextDocument)
        .options(frameOptions)
        .get
  }

  /** cf http://www.mastertheboss.com/javaee/json/how-to-pretty-print-a-jsonobject-using-jakarta-ee-api */
  def makeWriterFactory(): JsonWriterFactory = {
    val jsonWriterProperties = new java.util.HashMap[String, Any](1)
    jsonWriterProperties.put(JsonGenerator.PRETTY_PRINTING, true)
    Json.createWriterFactory(jsonWriterProperties)
  }
  def printJsonArray(jsa: JsonArray) {
    val sw = new StringWriter()
    val writerFactory = makeWriterFactory()
    val jsonWriter = writerFactory.createWriter(sw)
    jsonWriter.writeArray(jsa)
    jsonWriter.close()
    sw.close()
    println(sw.toString())
  }

  def printTitanium(titanium: RdfDataset) {
    println("======== titaniumOut =========")
    println(titanium.toList().asScala.
      map { t =>
        (
          t.getSubject,
          t.getSubject.isBlankNode(),
          t.getPredicate,
          t.getObject,
          t.getObject.isBlankNode())
      }.mkString("\n"))
  }
  def printTitaniumKO(titanium: RdfDataset) {
    val ds = DatasetFactory.create().asDatasetGraph()
    Titanium2Jena.populateDataset( titanium, ds)
   val fw = new FileWriter("printTitanium.ttl")
    RDFDataMgr.write( fw, ds, RDFFormat.TRIG )
    println( s"printTitanium written")
  }
}
