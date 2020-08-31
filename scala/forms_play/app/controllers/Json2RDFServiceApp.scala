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

import org.apache.jena.riot.RDFDataMgr
import java.io.ByteArrayOutputStream
import deductions.runtime.utils.URLReader
import deductions.runtime.services.CORS
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.StringWriter

class Json2RDFServiceApp
 extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  with Services
  with RDFStoreLocalJenaProvider // TODO remove, useless
  with CORS
  with URLReader {

  def json2rdf() = Action { implicit request: Request[_] =>
    val httpRequest = copyRequest(request)
    val jsonURL = httpRequest.getHTTPparameterValue("src").get
    val contextURLdefault = "https://raw.githubusercontent.com/jmvanel/rdf-convert/master/geonature/context.jsonld"
    val contextURL = httpRequest.getHTTPparameterValue("context").getOrElse(contextURLdefault)
    
    val processed = Try {
    val is1 = getRestInputStream(jsonURL).get

    val options = new JsonLdOptions()
    if( contextURL != "" ) {
      val is2 = getRestInputStream(contextURL).get
      options.setExpandContext(JsonDocument of(is2))
    }
    // be able to process media type 'text/plain' , use JsonDocument
    // https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/document/JsonDocument.html
    // toRdf Supports only content types [application/ld+json, application/json, +json, application/n-quads]]
    val titaniumDataset = JsonLd.toRdf(JsonDocument of(is1)).
      options(options).
//      base("https://api.inaturalist.org/v1/observations/").
//      base(jsonURL).
      get
      logger.info(s"json2rdf: after toRdf , titaniumDataset size() ${titaniumDataset.size()}")
    val dataset = DatasetFactory.create()
    val datasetGraph = dataset.asDatasetGraph()
    Titanium2Jena.populateDataset(titaniumDataset, datasetGraph)
    logger.whenDebugEnabled {
      val prw = new StringWriter
      RDFDataMgr.write(prw, datasetGraph, org.apache.jena.riot.RDFFormat.NQUADS_UTF8)
      println(prw.toString())
    }
    logger.info(s"json2rdf: after populateDataset datasetGraph size ${datasetGraph.size()}")
    val outputStream = new ByteArrayOutputStream
    // TODO detect when the output is multi graph RDF and then use N-Triples format
    val modelToWrite = dataset.getUnionModel
    logger.info(s"json2rdf: modelToWrite size: ${modelToWrite.size()}")
    RDFDataMgr.write(outputStream, modelToWrite, org.apache.jena.riot.RDFFormat.TURTLE )
//    val stringWriter = new  StringWriter
//    RDFDataMgr.write(stringWriter, dataset.getDefaultModel, org.apache.jena.riot.RDFFormat.TURTLE )
        // NQUADS) // NTRIPLES) // 
//      logger.info(s"json2rdf: after RDFDataMgr.write stringWriter '$stringWriter'")
      outputStream
    }
    processed match {
      case Success(os) =>
        logger.info(s"json2rdf: Success(os)")
        val resp = os.toString("UTF-8")
        //        logger.info(s"after resp, ${resp.substring(0, Math.min(1000, resp.length() -1))}")
        Ok(resp)
          //        .as("application/n-quad; charset=utf-8")
          .as("text/turtle; charset=utf-8")
          .withHeaders(corsHeaders.toList: _*)
      case Failure(f) =>
        val mess = f match {
          case e: JsonLdError => e.getCode().toString() + "\n" + e.getMessage
          case _              => f.toString()
        }
        InternalServerError(mess) //  fillInStackTrace())
    }
  }
}