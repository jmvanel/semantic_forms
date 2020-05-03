package deductions.runtime.sparql_cache

import java.io.FileInputStream
import com.github.jsonldjava.utils.JsonUtils
import java.util.HashMap
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor

object JsonTest extends App {
  // Open a valid json(-ld) input file
  val inputStream = new FileInputStream(args(0));
  // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
  // Number or null depending on the root object in the file).
  val jsonObject = JsonUtils.fromInputStream(inputStream);
  // Create a context JSON map containing prefixes and definitions
  val context = new HashMap();

  // Customise context...
  // Create an instance of JsonLdOptions with the standard JSON-LD options
  val options = new JsonLdOptions()
  options.setProcessingMode(JsonLdOptions.JSON_LD_1_1)

  // Customise options...
  // Call whichever JSONLD function you want! (e.g. compact)
  val compact = JsonLdProcessor.compact(jsonObject, context, options);
  // Print out the result (or don't, it's your call!)
  System.out.println(JsonUtils.toPrettyString(compact));
}