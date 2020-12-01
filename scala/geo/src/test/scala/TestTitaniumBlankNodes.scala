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

/** run with
 *  test:runMain TestTitaniumBlankNodes
 *  */
object TestTitaniumBlankNodes extends App {
  val rdfProvider = DefaultRdfProvider.INSTANCE
  val subject = rdfProvider.createIRI("urn:s1")
  val objet = rdfProvider.createBlankNode("bn1")
  val predicate = rdfProvider.createIRI("urn:p1")
  val titaniumDS: RdfDataset = rdfProvider.createDataset()
  val nquad = rdfProvider.createNQuad(subject, predicate, objet, null)
  titaniumDS.add(nquad)

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