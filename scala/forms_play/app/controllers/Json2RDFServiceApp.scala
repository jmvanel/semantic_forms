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
//import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.jsonld._
import com.apicatalog.jsonld.api._
import com.apicatalog.jsonld.document._

import org.apache.jena.riot.RDFDataMgr
import java.io.ByteArrayOutputStream
import deductions.runtime.utils.URLReader

class Json2RDFServiceApp
 extends {
    override implicit val config = new PlayDefaultConfiguration
  }
  with Services
  with RDFStoreLocalJenaProvider
  with URLReader
{
  def json2rdf() = Action { implicit request: Request[_] =>
    val httpRequest = copyRequest(request)
    //          Titanium2Jena.populateDataset( RdfDataset titaniumIn, DatasetGraph jenaToUpdate)
    val jsonURL = httpRequest.getHTTPparameterValue("src").get
    val contextURLdefault = "https://raw.githubusercontent.com/jmvanel/rdf-convert/master/geonature/context.jsonld"
    val contextURL = httpRequest.getHTTPparameterValue("context").getOrElse(contextURLdefault)
    val is1 = getRestInputStream(jsonURL).get
    val is2 = getRestInputStream(contextURL).get

    val options = new JsonLdOptions()
    options.setExpandContext(JsonDocument of(is2))
    // be able to process media type 'text/plain' , use JsonDocument
    // https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/document/JsonDocument.html
    // toRdf Supports only content types [application/ld+json, application/json, +json, application/n-quads]]
    val titaniumDataset = JsonLd.toRdf(JsonDocument of(is1)).
      options(options).get
    val dataset = DatasetFactory.create()
    val datasetGraph = dataset.asDatasetGraph()
    Titanium2Jena.populateDataset(titaniumDataset, datasetGraph)
    val outputStream = new ByteArrayOutputStream
    RDFDataMgr.write(outputStream, dataset.getDefaultModel,
        org.apache.jena.riot.RDFFormat. // NQUADS) // NTRIPLES) // 
        TURTLE );

    Ok(outputStream.toString("UTF-8"))
//      .as("text/plain; charset=utf-8")
      .as("application/n-quad; charset=utf-8")
      .withHeaders(corsHeaders.toList: _*)
  }
}