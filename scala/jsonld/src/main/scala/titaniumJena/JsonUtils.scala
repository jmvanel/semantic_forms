package titaniumJena

import java.util.HashMap
import jakarta.json.stream.JsonGenerator
import java.io.StringWriter
import jakarta.json.Json
import jakarta.json.JsonObject

trait JsonUtils {
  def prettyPrintJSON(obj: JsonObject): String = {
    val properties =
      new HashMap[String, Any](1)
    //				new HashMap<String,*>(1);
    properties.put(JsonGenerator.PRETTY_PRINTING, true)
    val sw = new StringWriter()
    val writerFactory = Json.createWriterFactory(properties);
    val jsonWriter = writerFactory.createWriter(sw)
    jsonWriter.writeObject(obj)
    jsonWriter.close();
    //				System.out.println(
    sw.toString()
  }
}