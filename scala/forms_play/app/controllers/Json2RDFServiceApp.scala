package controllers

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results._

import deductions.runtime.jena.RDFStoreLocalJenaProvider
import org.apache.jena.query.DatasetFactory
import titaniumJena._
import com.apicatalog.jsonld._
import com.apicatalog.jsonld.api._
import com.apicatalog.jsonld.document._

//import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import java.io.ByteArrayOutputStream
import deductions.runtime.utils.URLReader
import deductions.runtime.services.CORS
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.StringWriter

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController
import jakarta.json.JsonArray
import jakarta.json.Json

import java.io.PrintWriter
import com.apicatalog.jsonld.http.media.MediaType
//import deductions.runtime.jena.GraphWriterPrefixMap
import deductions.runtime.sparql_cache.GraphWriterPrefixMap
import java.net.URI
import titaniumJena.JsonUtils
import deductions.runtime.utils.Timer
import scala.io.Source
import java.util.HashMap
import com.apicatalog.jsonld.expansion.ObjectExpansion1314
import java.util.logging.Level

class Json2RDFServiceApp @Inject() (
     components: ControllerComponents, configuration: play.api.Configuration)
extends { override implicit val config = new PlayDefaultConfiguration(configuration)}
with AbstractController(components)
with HTTPrequestHelpers
with RDFStoreLocalJenaProvider // for prefix2uriMap
  with CORS
  with URLReader
  with GraphWriterPrefixMap
  with JsonUtils
  with Timer {

  def json2rdf() = Action { implicit request: Request[_] =>
    val httpRequest = copyRequest(request)
    val jsonURL = httpRequest.getHTTPparameterValue("src").get
    val timeout = httpRequest.getHTTPparameterValue("timeout").getOrElse("1000").toInt

    val contextURLdefault =
      // "file:///home/jmv/ontologies/Karstlink-ontology/geb.ffspeleo.fr_context.jsonld"
//       "file:///home/jmv/ontologies/Karstlink-ontology/grottocenter.org_context.jsonld"
      "https://ontology.uis-speleo.org/grottocenter.org_context.jsonld"
      // "https://github.com/jmvanel/Karstlink-ontology/raw/master/grottocenter.org_context.jsonld"

    import java.util.logging.Logger;
//    val LOGGER = Logger.getLogger(ObjectExpansion1314.getclass.getName());
    val LOGGER = Logger.getLogger( "com.apicatalog.jsonld.expansion.ObjectExpansion1314")
    LOGGER.setLevel(Level.FINE)

    lazy val contextJsonDocumentDefault: JsonDocument = {
      val factory = Json.createReaderFactory(new HashMap())
      val reader = factory.createReader(
        Source.fromURL(contextURLdefault).reader())
      val structure = reader.read()
      JsonDocument of (MediaType.JSON_LD, structure)
    }

    logger.info( "context: '" + httpRequest.getHTTPparameterValue("context") + "'" )
    val (contextURL, isDefault) = {
      val v = httpRequest.getHTTPparameterValue("context").getOrElse(contextURLdefault)
      if (v == "") (contextURLdefault, true)
      else if (v == contextURLdefault) (v, true)
      else (v, false)
    }

    val timerCall = {
      var ret = false
      logger.whenDebugEnabled{ ret=true }
      ret
    }
    val processed = Try {
      val contextJsonDocument = time(s"jsonLDcontextStream <$jsonURL>", {
        if (isDefault) {
//          println(s"contextURL isDefault ")
          contextJsonDocumentDefault
        } else {
          val jsonLDcontextStream = getRestInputStream(contextURL).get
          JsonDocument of (
            MediaType.JSON_LD, jsonLDcontextStream)
        }
      }, timerCall)
      val options = new JsonLdOptions()
      options.setExpandContext(contextJsonDocument)
      options.setBase(new URI(jsonURL))

      val titaniumDataset = time(s"toRdf <$jsonURL>", {
      // be able to process media type 'text/plain' , use JsonDocument
      // https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/document/JsonDocument.html
      val jsonDataStream = getRestInputStream(jsonURL,
          // for https://beta.grottocenter.org/api/v1/massifs/3
          connectionTimeout=timeout,
          socketTimeout=timeout).get
      JsonLd.toRdf(JsonDocument of (
          MediaType.JSON_LD, jsonDataStream )).
        options(options).
        numericId(). get
      }, timerCall)

      time(s"writeGraph <$jsonURL>", {
        logger.debug(s"json2rdf: after toRdf , titaniumDataset size() ${titaniumDataset.size()}")
        val dataset = DatasetFactory.create()
        val datasetGraph = dataset.asDatasetGraph()
        Titanium2Jena.populateDataset(titaniumDataset, datasetGraph)

        logger.info(s"json2rdf: after populateDataset datasetGraph size ${datasetGraph.size()}")
        // TODO detect when the output is multi graph RDF and then use N-Triples format
        val modelToWrite = dataset.getUnionModel

        logger.info(s"json2rdf: modelToWrite size: ${modelToWrite.size()}")
        import scala.collection.JavaConverters._
        val outputStream = new ByteArrayOutputStream
        // RDFDataMgr.write(outputStream, modelToWrite, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
        writeGraph(modelToWrite.getGraph, outputStream,
          RDFFormat.TURTLE_PRETTY, prefix2uriMap.asJava,
          jsonURL)
        outputStream
      }, timerCall)
    }

    processed match {
      case Success(os) =>
        logger.info(s"json2rdf: Success(os)")
        val resp = os.toString("UTF-8")
        // logger.info(s"after resp, ${resp.substring(0, Math.min(1000, resp.length() -1))}")
        Ok(resp)
          // .as("application/n-quad; charset=utf-8")
          .as("text/turtle; charset=utf-8")
          .withHeaders(corsHeaders.toList: _*)

      case Failure(f) =>
        val mess = f match {
          case e: JsonLdError => 
            e.printStackTrace
            e.getCode().toString() + "\n" +
            e.getMessage + "\n" +
            "Cause: " + e.getCause
          case _ =>
            val baos = new ByteArrayOutputStream
            val ss = new PrintWriter(baos)
            f.printStackTrace(ss)
            ss.close
            baos.close()
//            f.toString() + "\n" +
              baos.toString()
        }
        logger.error( s"<$jsonURL> " + f.getLocalizedMessage)
        InternalServerError(mess) //  fillInStackTrace())
    }
  }

  def json2rdfHead() = Action { implicit request: Request[_] =>
    Ok("")
      .as("text/turtle; charset=utf-8")
      .withHeaders(corsHeaders.toList: _*)
  }
}
