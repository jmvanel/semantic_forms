package deductions.runtime.connectors

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import java.net.{HttpURLConnection, URL}

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration

/**
 * App to transform CSV into a Turtle file;
 *  args:
 *  0 - URL or File of CSV,
 *  1 - base URL for the rows,
 *  2 - URL or File ( in Turtle ) for adding details to each row; for example it contains:
 *  		<any:ROW> a foaf:Person .
 *    which will add this triple to every row.
 *  3 separator ( comma by default)
 *
 * Features: like Any23, plus:
 * - abbreviated Turtle terms with well-known prefixes (eg foaf:name) are understood as columns names
 * - abbreviated Turtle terms with well-known prefixes (eg dbpedia:Paris) are understood in cells
 */
object CSVImporterApp extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery: Boolean = true // false
  }
} with App
    with ImplementationSettings.RDFCache
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import ops._

  if(args.length < 1) {
    println("""This application transforms CSV into a Turtle file; the args are:
0 - URL or File of CSV,
1 - URI prefix for the rows (facultative),
2 - URL or File ( in Turtle ) for adding details to each row (facultative); for example it contains:
	`<any:ROW> a foaf:Person .`
3 - separator in CSV (facultative)
4 - graph URI where to put the triples (facultative; can be user:user_name )
""")
    System.exit(0)
  }
  println(s"Arguments: ${args.mkString(", ")}")
  val urlOrFile = args(0)

  // make URL from file or already URL
  val url = if (urlOrFile.startsWith("http") ||
    urlOrFile.startsWith("https"))
    urlOrFile
  else {
    val file = new File(urlOrFile)
    if (file.exists()) file.toURI().toURL().toString()
    else throw new RuntimeException(s"File $file does not exist!")
  }

  // make URI prefix
  val uriPrefix: Rdf#URI = URI(
    if (args.size > 1) args(1) else {
      val d = url match {
        case _ if (url.endsWith("#")) => url
        case _ if (url.endsWith("/")) => url
        case _ => url + "#"
      }
      d
    })
  println(s"""URI prefix $uriPrefix""")

  val in: InputStream = getUrlInputStream(url)
  val graph = if (args.size > 2) {
    val propertyValueForEachRow: List[(Rdf#URI, Rdf#Node)] = {
      val eachRowFile = args(2)
      println(s"Opening TTL file for adding triples about each row: $eachRowFile")
      val graph = turtleReader.read(new FileInputStream(eachRowFile), "")
      val r = for (triple <- graph.getOrElse(emptyGraph).triples) yield {
        (triple.predicate, triple.objectt)
      }
      r.toList
    }
  val separator = if (args.size > 3) args(3)(0) else ','
	  println(s"separator '$separator'")
	  run(in, uriPrefix,
      propertyValueForEachRow, separator)
  } else
    run(in, uriPrefix)

  {
    val outputFile = urlOrFile + ".ttl"
    println(s"""Write $outputFile,
      # of triples ${graph.size()}""")
    val os = new FileOutputStream(outputFile)
    turtleWriter.write(graph, os, "") // fromUri(documentURI))
  }
  println(s"args ${args.mkString(", ")}" )
  if (args.size > 4) {
    val graphURI = URI(args(4))
    println(s"""populate graph URI <$graphURI>""")
//    rdfStore.removeGraph(dataset, graphURI)
    rdfStore.appendToGraph(dataset, graphURI, graph)
  }

  /**
   * See http://alvinalexander.com/blog/post/java/how-open-url-read-contents-http...
   * Note that this can throw a java.net.UnknownHostException
   */
  def getUrlInputStream(url: String,
    connectTimeout: Int = 5000,
    readTimeout: Int = 5000,
    requestMethod: String = "GET") = {
    val u = new URL(url)
    val conn = u.openConnection
    HttpURLConnection.setFollowRedirects(false)
    conn.setConnectTimeout(connectTimeout)
    conn.setReadTimeout(readTimeout)
    conn match {
      case conn: HttpURLConnection => conn.setRequestMethod(requestMethod)
      case _ =>
    }
    conn.connect
    conn.getInputStream
  }

}
