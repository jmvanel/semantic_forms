package titaniumJena

import com.apicatalog.jsonld._
import com.apicatalog.jsonld.api._
import com.apicatalog.rdf._

import scala.collection.JavaConverters._
import org.apache.jena.query.DatasetFactory
import java.io.ByteArrayOutputStream
import org.apache.jena.riot.RDFDataMgr

/** apply to given JSON URL given JSON-LD @context URL */
object Titanium2RDF extends App {

  def jsonLDtoRDF3(uri: String, context: String): RdfDataset = {
    val options = new JsonLdOptions()
    options.setExpandContext(context)
    JsonLd.toRdf(uri).options(options).get
  }

  val jsonldInput = args(0)
  val jsonldContext = args(1)
  try{
  val ds = jsonLDtoRDF3(jsonldInput, jsonldContext)

  // cf class Json2RDFServiceApp
  val dataset = DatasetFactory.create()
  val datasetGraph = dataset.asDatasetGraph()
  Titanium2Jena.populateDataset(ds, datasetGraph)
  println(s"json2rdf:  after populateDataset  size ${datasetGraph.size()}")
  val outputStream = new ByteArrayOutputStream
  RDFDataMgr.write(System.out, dataset.getDefaultModel, org.apache.jena.riot.RDFFormat.TURTLE)
}
catch { case e: JsonLdError => println( e.getCode() .toString() + "\n" + e.getMessage )
        case f:  Throwable  => println(f.toString() )
  }
}