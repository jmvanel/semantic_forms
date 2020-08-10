package controllers

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results._

import deductions.runtime.jena.RDFStoreLocalJenaProvider
import org.apache.jena.rdf.model.ModelFactory
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
    val is2 = getRestInputStream(contextURL).get

    val options = new JsonLdOptions()
    options.setExpandContext(JsonDocument of(is2))
    // be able to process media type 'text/plain' , use JsonDocument
    // https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/document/JsonDocument.html
    // toRdf Supports only content types [application/ld+json, application/json, +json, application/n-quads]]
    val titaniumDataset = JsonLd.toRdf(JsonDocument of(is1)).
      options(options).get
       println(s"json2rdf: after toRdf , size() ${titaniumDataset.size()}")
   val dataset = DatasetFactory.create()
    val datasetGraph = dataset.asDatasetGraph()
    Titanium2Jena.populateDataset(titaniumDataset, datasetGraph)
      println(s"json2rdf:  after populateDataset  size ${datasetGraph.size()}")
    val outputStream = new ByteArrayOutputStream
    RDFDataMgr.write(outputStream, dataset.getDefaultModel, org.apache.jena.riot.RDFFormat.TURTLE )
//    val stringWriter = new  StringWriter
//    RDFDataMgr.write(stringWriter, dataset.getDefaultModel, org.apache.jena.riot.RDFFormat.TURTLE )
        // NQUADS) // NTRIPLES) // 
//      println(s"json2rdf: after RDFDataMgr.write stringWriter '$stringWriter'")
      outputStream
    }
    processed match {
      case Success(os) =>
        println(s"json2rdf: Success(os)")
        val resp = os.toString("UTF-8")
//        println(s"after resp, ${resp.substring(0, Math.min(1000, resp.length() -1))}")
        println(s"after resp, length ${resp.length() }")
        Ok(resp)
//        .as("application/n-quad; charset=utf-8")
          .as("text/turtle; charset=utf-8")
          .withHeaders(corsHeaders.toList: _*)
      case Failure(f) => InternalServerError(f.toString()) //  fillInStackTrace())
    }
  }
}