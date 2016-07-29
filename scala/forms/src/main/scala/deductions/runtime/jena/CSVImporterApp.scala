package deductions.runtime.jena

import deductions.runtime.utils.CSVImporter
import java.io.InputStream
import java.net.URL
import java.net.HttpURLConnection
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import deductions.runtime.services.DefaultConfiguration

/**
 * App to Import CSV into TDB;
 *  args:
 *  0 - URL Or File of CSV,
 *  1 - base URL for the rows,
 *  2 - import details URL or File ( in Turtle ) ; for example it contains:
 *  		<any:ROW> a foaf:Person .
 *    which will add this triple to every row.
 */
object CSVImporterApp extends App
    with RDFStoreLocalJena1Provider
    with DefaultConfiguration
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  import ops._

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
  val documentURI: Rdf#URI = URI(
    if (args.size > 1) args(1) else {
      //      url })
      val d = url match {
        case _ if (url.endsWith("#")) => url
        case _ if (url.endsWith("/")) => url
        case _ => url + "#"
      }
      d
    })
  println(s"""document URI
		  $documentURI""")

  val in: InputStream = getUrlInputStream(url)
  val graph = if (args.size > 2) {
    val propertyValueForEachRow: List[(Rdf#URI, Rdf#Node)] = {
      val eachRowFile = args(2)
      println(s"Opening TTL file for adding triples about each row: $eachRowFile")
      val graph = turtleReader.read(new FileReader(eachRowFile), "")
      val r = for (triple <- graph.getOrElse(emptyGraph).triples) yield {
        (triple.predicate, triple.objectt)
      }
      r.toList
    }
    run(in, documentURI,
      propertyValueForEachRow)
  } else
    run(in, documentURI)

  //  if (args.size > 3 && args(3) == "print" ) 
  {
    val outputFile = urlOrFile + ".ttl"
    println(s"""Write $outputFile,
      # of triples ${graph.size()}""")
    val os = new FileOutputStream(outputFile)
    turtleWriter.write(graph, os, fromUri(documentURI))
  }
  rdfStore.appendToGraph(dataset, documentURI, graph)

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
