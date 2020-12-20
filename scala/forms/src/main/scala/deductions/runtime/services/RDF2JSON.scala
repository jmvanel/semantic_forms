package deductions.runtime.services

import deductions.runtime.core.SemanticControllerGeneric
import titaniumJena._
import com.apicatalog.jsonld._
import com.apicatalog.jsonld.api._
import com.apicatalog.jsonld.document._
import deductions.runtime.core.HTTPrequest
import deductions.runtime.utils.URLReader
import scala.util.Try
import com.apicatalog.jsonld.http.media.MediaType
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.graph.GraphFactory

/**  From RDF URL, get JSON-LD ,ad then apply frame algorithm */
trait RDF2JSON extends SemanticControllerGeneric[Try[String]]
with URLReader
with JsonUtils {
  def result(request: HTTPrequest): Try[String] = {
   val rdfURL = request.getHTTPparameterValue("src").get
    val frameURLdefault = "https://github.com/jmvanel/Karstlink-ontology/raw/master/grottocenter.org_frame.jsonld"
    logger.info( "frame: '" + request.getHTTPparameterValue("frame") + "'" )
    val frameURL = {
      val v = request.getHTTPparameterValue("frame").getOrElse(frameURLdefault)
      if (v == "") frameURLdefault
      else v
    }
    val processed = Try {
      val graph = GraphFactory.createDefaultGraph()
      RDFDataMgr.read(graph, rdfURL)
      val titaniumOut = com.apicatalog.rdf.impl.DefaultRdfProvider.INSTANCE.createDataset
      Jena2Titanium.populateDataset( graph, titaniumOut)
      // val rdfDataStream = getRestInputStream(rdfURL).get
      val options = new JsonLdOptions()
      logger.info("Before fromRdf")
      val jsonArray = JsonLd.fromRdf(RdfDocument of ( titaniumOut
//        MediaType.N_QUADS, rdfDataStream)
      )). options(options).get
      
      val jsonDocument = JsonDocument of (jsonArray)
      
      logger.info("Before frameJsonDocument")
      val jsonLDframeStream = getRestInputStream(frameURL).get
      val frameJsonDocument = JsonDocument of (
        MediaType.JSON_LD, jsonLDframeStream)

      logger.info("Before frame")
      val outJsonObject = JsonLd.frame(jsonDocument, frameJsonDocument) . get
      prettyPrintJSON(outJsonObject)
    }
    processed
  }

}