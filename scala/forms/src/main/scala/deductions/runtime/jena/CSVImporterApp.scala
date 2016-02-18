package deductions.runtime.jena

import deductions.runtime.utils.CSVImporter
import java.io.InputStream
import java.net.URL
import java.net.HttpURLConnection
import java.io.File
import java.io.FileOutputStream

/**
 * App to Import CSV into TDB;
 *  args: url Or File of CSV,
 *  output document URI (also base URL for the rows)
 */
object CSVImporterApp extends App
    with RDFStoreLocalJena1Provider
    with CSVImporter[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  import ops._

  val urlOrFile = args(0)
  val url = if (urlOrFile.startsWith("http") ||
    urlOrFile.startsWith("https"))
    urlOrFile
  else {
    val file = new File(urlOrFile)
    if (file.exists()) file.toURI().toURL().toString()
    else throw new RuntimeException(s"File $file does not exist!")
  }

  val in: InputStream = getUrlInputStream(url)
  val documentURI: Rdf#URI = URI(
    if (args.size > 1) args(1) else url)
  val graph = run(in, documentURI): Rdf#Graph

//  if (true args.size > 2 && args(2) == "print" ) 
  {
    val outputFile = urlOrFile + ".ttl"
    println(s"Write $outputFile, # of triples ${graph.size()}")
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