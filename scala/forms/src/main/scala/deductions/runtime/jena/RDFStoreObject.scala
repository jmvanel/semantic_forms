package deductions.runtime.jena

import java.io.File
import java.nio.file.Paths

import deductions.runtime.jena.lucene.LuceneIndex
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.Timer
import org.apache.jena.graph.{Graph => JenaGraph, Node => JenaNode, Triple => JenaTriple}
import org.apache.jena.query.{DatasetFactory, Query => JenaQuery}
import org.apache.jena.riot.RiotException
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.tdb.transaction.TransactionManager
import org.apache.log4j.Logger
import org.w3.banana.jena.{Jena, JenaDatasetStore, JenaModule}

import scala.collection.JavaConversions.asScalaIterator
import scala.util.Try
import org.w3.banana.jena.io.TripleSink
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFLanguages
import org.apache.http.impl.client.cache.CachingHttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import scala.util.Failure
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import org.apache.commons.io.IOUtils
import java.io.StringReader
import deductions.runtime.services.RDFContentNegociationIO

/**
 * singleton for implementation settings
 */
object ImplementationSettings {
  // pave the way for migration to Jena 3 ( or BlazeGraph )
  type DATASET = org.apache.jena.query.Dataset
  type Rdf = Jena
  type RDFModule = JenaModule
  /** actually just RDF database location; TODO rename RDFDatabase */
  type RDFCache = RDFStoreLocalJena1Provider
  type RDFReadException = RiotException
}

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : TDB */
trait RDFStoreLocalJena1Provider
  extends RDFStoreLocalJenaProvider

/**
 * NOTES:
 * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
 */
trait RDFStoreLocalJenaProvider
    extends MicrodataLoaderModuleJena
    with ImplementationSettings.RDFModule
    with RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Timer
    with LuceneIndex
    with RDFContentNegociationIO[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  // CURRENTLY unused, but could be:  val config: Configuration
  import config._
  import ops._
  type DATASET = ImplementationSettings.DATASET
  /** very important that defensiveCopy=false, otherwise no update happens, and a big overhead for every operation */
  override val rdfStore = new JenaDatasetStore(false)

  /**
   * default is 10; each chunk commitedAwaitingFlush can be several Mb,
   *  so this can easily make an OOM exception
   */
  TransactionManager.QueueBatchSize = 5
  //  override TransactionManager.DEBUG = true

  /**
   * create (or re-connect to) TDB Database in given directory;
   *  if it is empty, create an in-memory Database
   */
  override def createDatabase(
      database_location: String,
      useTextQuery: Boolean = useTextQuery): ImplementationSettings.DATASET = {
    if (database_location != "") {

      // if the directory does not exist, create it
      val currentRelativePath = Paths.get("");
      val abs = currentRelativePath.toAbsolutePath().toString();
      System.out.println("Current relative path is: " + abs);
      val dir = new File(abs, database_location)
      if (!dir.exists()) {
        System.out.println("creating database directory: " +
          database_location + " as " + dir + " - current (.) : " + new File(".").getAbsolutePath);
        dir.mkdirs()
      }

      println(s"""RDFStoreLocalJena1Provider: dataset create: database_location "$database_location" in ${System.getProperty("user.dir")} """)
      val dts = TDBFactory.createDataset(Paths.get(database_location).toString())
      //      Logger.getRootLogger.info
      println(s"RDFStoreLocalJena1Provider $database_location, dataset created: $dts")

      try {
        val res = configureLuceneIndex(dts, useTextQuery)
        println(s"configureLuceneIndex DONE useTextQuery: $useTextQuery => $res")
        res
      } catch {
        case t: Throwable =>
          logger.error("!!!!! createDatabase: Exception: " + t.getLocalizedMessage)
          logger.error("	getCause " + t.getCause)
          logger.error("	>> Lucene will not be available.")
          dts
      }
    } else
      DatasetFactory.createTxnMem()
  }

  /**
   * NOTES:
   *  - NEEDS transaction
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  - Union Graph in Jena should be re-done for each use (not 100% sure, but safer anyway)
   */
  override def allNamedGraph: Rdf#Graph = {
    time(s"allNamedGraph dataset $dataset", {
      //      println(s"Union Graph: entering for $dataset")

      // NOTE: very important to use the properly configured rdfStore (with defensiveCopy=false)
      val ang = rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph")).get
      //      println(s"Union Graph: hashCode ${ang.hashCode()} : size ${ang.size}")
      ang
    })
    //    union(dataset.getDefaultModel.getGraph :: unionGraph :: Nil)
  }

  /** List the names of graphs */
  def listNames(ds: DATASET): Iterator[String] = ds.listNames()

  /** make an MGraph from a Dataset */
  def makeMGraph(graphURI: Rdf#URI, ds: DATASET = dataset): Rdf#MGraph = {
    logger.debug(s"makeMGraph( $graphURI")
    val nm = ds.getNamedModel(fromUri(graphURI))
    nm.getGraph
  }

  def close(ds: DATASET) = ds.close()

  private val requestConfig =
        RequestConfig.custom()
          .setConnectTimeout(10 * 1000)
          .setConnectionRequestTimeout(10 * 1000)
          .build()

  def readWithContentType(
    uri: Rdf#URI,
    contentType: String,
    dataset: DATASET): Try[Rdf#Graph] =
    readWithContentTypeNoJena(uri, contentType, dataset)

  private def readWithContentTypeJena(
    uri: Rdf#URI,
    contentType: String,
    dataset: DATASET): Try[Rdf#Graph] = {
    Try {
      val sink = new TripleSink
      import org.apache.jena.riot.LangBuilder
      val contentTypeNoEncoding = contentType.replaceFirst(";.*", "")
      val lang = RDFLanguages.contentTypeToLang(contentTypeNoEncoding)
      println(s"readWithContentType: $lang , contentType $contentType, contentTypeNoEncoding $contentTypeNoEncoding")
      RDFParser.create()
        .httpClient(
          CachingHttpClientBuilder.create()
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(requestConfig)
            .build())
        .source(fromUri(uri))
//        .httpAccept(contentType)
        .forceLang(lang)
//        .lang(lang)
        .parse(sink)
      sink.graph
    }
  }

  private def readWithContentTypeNoJena(
    uri: Rdf#URI,
    contentType: String,
    dataset: DATASET): Try[Rdf#Graph] = {
    val httpClient = CachingHttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy())
      .setDefaultRequestConfig(requestConfig)
      .build()
    val request = new HttpGet(fromUri(uri))

    // For Virtuoso :(
    val contentTypeNormalized = contentType.replaceAll("; .*", "")
    println(s"readWithContentTypeNoJena: contentTypeNormalized: $contentTypeNormalized ")

    request.addHeader("Accept", contentTypeNormalized   )
    val response = httpClient.execute(request)
    val inputStream = response.getEntity.getContent
    val reader = getReaderFromMIME(contentTypeNormalized)
    //  contentTypes2Readers.getOrElse(contentTypeNormalized, turtleReader)
    if( ! isKnownRdfSyntax(contentTypeNormalized) )
//        contentTypes2Readers.get(contentTypeNormalized).isEmpty )
      System.err.println(
            s"readWithContentTypeNoJena: no Reader found for contentType $contentType ; trying Turtle")

    println(s"""readWithContentTypeNoJena:
      reader $reader
      request $request , getAllHeaders ${
      for ( h <- request.getAllHeaders ) println(h)
      }
      response $response""")

    if( response.getStatusLine().getStatusCode != 200 )
      {
        val rdr = new InputStreamReader( inputStream )
        val writer = new StringWriter()
        IOUtils.copy(inputStream, writer, "utf-8")
        println(s"readWithContentTypeNoJena: response $writer")
      }

    println(s"readWithContentTypeNoJena: reader $reader =========")
    val res = reader.read(inputStream, fromUri(uri))

      if( res.isFailure ) {
        println(s"readWithContentTypeNoJena: reader.read() result: $res")
        val response = httpClient.execute(request)
        val inputStream = response.getEntity.getContent
        val rdr = new InputStreamReader( inputStream )
        val writer = new StringWriter()
        IOUtils.copy(inputStream, writer, "utf-8")
//        println(s"readWithContentTypeNoJena: response $writer")
        val rdfString = writer.toString()
        println(s"readWithContentTypeNoJena: rdfString ${rdfString.substring(0, 2000)}")
        reader.read(new StringReader(rdfString), fromUri(uri))
      } else
      res
  }

}

/** TODO implement independently of Jena */
trait RDFGraphPrinter extends RDFStoreLocalJena1Provider {
  def printGraphList() {
    rdfStore.r(dataset, {
      val lgn = dataset.asDatasetGraph().listGraphNodes()
      Logger.getRootLogger().info(s"listGraphNodes size ${lgn.size}")
      for (gn <- lgn) {
        Logger.getRootLogger().info(s"${gn.toString()}")
      }
      Logger.getRootLogger().info(s"afer listGraphNodes")
    })
  }
}
