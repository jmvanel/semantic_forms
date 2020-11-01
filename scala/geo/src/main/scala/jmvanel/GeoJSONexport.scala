package jmvanel

import org.apache.jena.tdb.TDBFactory
import org.apache.jena.query._
import collection.JavaConverters._
import java.io.FileWriter
import java.io.File
import org.apache.jena.riot.JsonLDWriteContext
import org.apache.jena.riot.RDFFormat
import java.net.URL
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.writer.JsonLDWriter
import org.apache.jena.riot.system.PrefixMapStd
import titaniumJena.Jena2Titanium
import com.apicatalog.rdf.impl.DefaultRdfProvider
import com.apicatalog.jsonld.JsonLd
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.RdfGraph
import com.apicatalog.jsonld.document.DocumentParser
import com.apicatalog.jsonld.http.media.MediaType
import java.io.StringReader
import com.apicatalog.jsonld.document.RdfDocument
import javax.json.Json
import javax.json.JsonObject
import com.apicatalog.jsonld.api.impl.FromRdfApi
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.api.JsonLdOptions

/** export GeoJSON (both plain GeoJSON & JSON-LD) from URI's having geographic data
 *  in given TDB database */
object GeoJSONexport extends App {
  // Open TDB db
  val dataset = TDBFactory.createDataset(args(0))

  // SPARQL query
  val queryString = """
  PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
  PREFIX geojson: <https://purl.org/geojson/vocab#>
  CONSTRUCT {

    <urn:geojson> a geojson:FeatureCollection ;
      geojson:features ?S .
   
    ?S a geojson:Feature ;
         geojson:geometry ?point .
      ?point  a geojson:Point ;
              geojson:coordinates ?coordinates .
    ?coordinates rdf:first ?LON ; rdf:rest ?rest .
    ?rest rdf:first ?LAT ;  rdf:rest   rdf:nil .
    ?S geojson:properties ?properties .
    ?properties ?P ?O .
    }
  WHERE {
    GRAPH ?gr {
      ?S geo:lat ?LAT .
      ?S geo:long ?LON .
      BIND(BNODE() AS ?point)
      BIND(BNODE() AS ?coordinates)
      BIND(BNODE() AS ?rest)
      BIND(BNODE() AS ?properties)
      ?S ?P ?O .
      FILTER ( ?P != geo:lat )
      FILTER ( ?P != geo:long )
    }
  } LIMIT 100
  """
  val fileName = "out.geojson"

  val qexec = QueryExecutionFactory.create(queryString, dataset)
  val resultModel = qexec.execConstruct()

  val datasetGraph = DatasetFactory.wrap(resultModel).asDatasetGraph()
  
  // From https://geojson.org/geojson-ld/geojson-context.jsonld
  val geojsonContext = """{
  "@context": {
    "geojson": "https://purl.org/geojson/vocab#",
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
  }
}"""
  val fw = new FileWriter(fileName)
  
  val rdfProvider = DefaultRdfProvider.INSTANCE
  val titaniumOut = rdfProvider.createDataset()
  Jena2Titanium.populateDataset( resultModel.getGraph, titaniumOut)
  val jsonObject = rdfToJsonLD(titaniumOut, geojsonContext)
  val jsonWriter = Json.createWriter(fw)
  jsonWriter.writeObject(jsonObject)
  jsonWriter.close()
  
  def rdfToJsonLD(titaniumOut: RdfDataset, context: String): JsonObject = {
    val options = new JsonLdOptions()
    options.setUseNativeTypes(true)
    options.setOmitGraph(true)
    options.setCompactToRelative(true)
    options.setUseNativeTypes(true)
    val inputStream = new StringReader(context)
    val contextDocument = DocumentParser.parse(
        MediaType.JSON_LD,
        inputStream)
    val fromRdf: FromRdfApi =
      JsonLd.fromRdf(
        RdfDocument.of(titaniumOut) )
      JsonLd.frame(
      // compact(
        JsonDocument.of(fromRdf.get), contextDocument)
        .options(options)
        .get
  }
}
