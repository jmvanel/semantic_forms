import com.apicatalog.rdf.impl.DefaultRdfProvider
import com.apicatalog.jsonld.api.impl.FromRdfApi
import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.document.RdfDocument
import com.apicatalog.rdf.RdfDataset
//import javax.json.JsonArray
//import javax.json.Json
import jakarta.json.JsonArray
import jakarta.json.Json
import java.io.StringWriter
import com.apicatalog.rdf.Rdf

/** run with
 *  test:runMain TestTitaniumBlankNodes2
 *  */
object TestTitaniumBlankNodes2 extends App {

  val titaniumDS  = Rdf.createDataset()
                                        .add(Rdf.createTriple(
                                                    Rdf.createIRI("urn:s1"), 
                                                    Rdf.createIRI("urn:p1"),
                                                    Rdf.createBlankNode("bn1")
                                                ))

  val fromRdf: FromRdfApi =
      JsonLd.fromRdf(
        RdfDocument.of(titaniumDS) )

    printJsonArray(fromRdf . get) // KO: blank node as IRI !!!!!!!!!!!!!!!!!!!!

  def printJsonArray(jsa: JsonArray) {
    val sw = new StringWriter()
    val factory = Json.createWriterFactory(new java.util.HashMap() )
    val jsonWriter = factory.createWriter(sw)
    jsonWriter.writeArray(jsa)
    jsonWriter.close()
    sw.close()
    println(sw.toString())
  }
}