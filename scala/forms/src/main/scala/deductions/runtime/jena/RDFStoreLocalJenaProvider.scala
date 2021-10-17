package deductions.runtime.jena

import java.io.File
import java.nio.file.Paths
import java.io.InputStreamReader

import deductions.runtime.jena.lucene.LuceneIndex
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.Timer
//import org.apache.jena.graph.{Graph => JenaGraph, Node => JenaNode, Triple => JenaTriple}
import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.RiotException
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.tdb.transaction.TransactionManager
import org.w3.banana.jena.{Jena, JenaDatasetStore, JenaModule}

import scala.collection.JavaConverters._

import scala.util.Try
import org.w3.banana.jena.io.TripleSink
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFLanguages
import org.apache.http.impl.client.cache.CachingHttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import scala.util.Failure
import java.io.StringWriter
import org.apache.commons.io.IOUtils
import java.io.StringReader
import deductions.runtime.services.RDFContentNegociationIO
import scala.util.Success
import deductions.runtime.utils.HTTPHelpers
import deductions.runtime.utils.DefaultConfiguration
import org.w3.banana.RDFStore
import org.apache.jena.tdb.TDB


/**
 * singleton for implementation settings
 */
object ImplementationSettings {
  // pave the way for migration to Jena 3 ( or BlazeGraph )
  type DATASET = org.apache.jena.query.Dataset
  type Rdf = Jena
  type RDFModule = JenaModule
  /** actually just RDF database location; TODO rename RDFDatabase */
  type RDFCache = RDFStoreLocalJenaProvider
  type RDFReadException = RiotException
}

trait RDFStoreLocalJenaProvider extends RDFStoreLocalJenaProviderImpl
    with MicrodataLoaderModuleJena
    with ImplementationSettings.RDFModule
//    with RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Timer
    with LuceneIndex
    with RDFContentNegociationIO[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with HTTPHelpers
    {
      override val rdfStore = RDFStoreLocalJenaProviderObject . rdfStore
    }

/** delegation to singleton RDFStoreLocalJenaProviderObject */
trait RDFStoreLocalJenaProviderImpl {
  val delegate = RDFStoreLocalJenaProviderObject
  // def rdfStore = delegate.rdfStore
  val rdfStore = delegate.rdfStore

  // Members declared in deductions.runtime.utils.RDFStoreLocalProvider
  def allNamedGraph = delegate.allNamedGraph
  def close(ds: deductions.runtime.jena.ImplementationSettings.DATASET = dataset): Unit = delegate.close(ds)
  def createDatabase(database_location: String,useTextQuery: Boolean,useSpatialIndex: Boolean):
  deductions.runtime.jena.ImplementationSettings.DATASET = delegate.createDatabase(database_location, useTextQuery, useSpatialIndex)
  def listNames(ds: deductions.runtime.jena.ImplementationSettings.DATASET): Iterator[String] = delegate.listNames(ds)
  def makeMGraph(graphURI: deductions.runtime.jena.ImplementationSettings.Rdf#URI,ds:
      deductions.runtime.jena.ImplementationSettings.DATASET): deductions.runtime.jena.ImplementationSettings.Rdf#MGraph = delegate.makeMGraph(graphURI, ds)
  def readWithContentType(uri: deductions.runtime.jena.ImplementationSettings.Rdf#URI,contentType: String,
      dataset: deductions.runtime.jena.ImplementationSettings.DATASET):
      scala.util.Try[deductions.runtime.jena.ImplementationSettings.Rdf#Graph] = delegate.readWithContentType(uri, contentType, dataset)

  lazy val dataset = delegate.dataset
  val jenaComplements = new JenaComplements() // (delegate.ops)
  val  graphWriter = new deductions.runtime.sparql_cache.GraphWriterPrefixMapClass
  
  def closeAllTDBs() = delegate.closeAllTDBs
  def syncTDB(ds: deductions.runtime.jena.ImplementationSettings.DATASET = dataset ) = delegate.syncTDB(ds)
}

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : TDB
 * NOTES:
 * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
 */
// class
object RDFStoreLocalJenaProviderObject
//MicrodataLoaderModuleJena
    extends ImplementationSettings.RDFModule
    with RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Timer
    with LuceneIndex
    with RDFContentNegociationIO[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with HTTPHelpers 
    {

  override implicit val config = new DefaultConfiguration{}
  import config._

  import ops._
  type DATASET = ImplementationSettings.DATASET
  /** very important that defensiveCopy=false, otherwise no update happens, and a big overhead for every operation */
  override val rdfStore
  // : RDFStore[ImplementationSettings.Rdf, Try, ImplementationSettings.DATASET] 
  // :RDFStore[Jena, Try, org.apache.jena.query.Dataset]
  = new JenaDatasetStore(false)
  val jenaComplements = new JenaComplements()//(ops)

  /**
   * default is 10; each chunk commitedAwaitingFlush can be several Mb,
   *  so this can easily make an OOM exception
   */
  TransactionManager.QueueBatchSize = 0 // same as Fuseki, advice of Andy
  //  override TransactionManager.DEBUG = true

  /**
   * create (or re-connect to) TDB Database in given directory;
   *  if it is empty, create an in-memory Database
   */
  override def createDatabase(
      database_location: String,
      useTextQuery: Boolean = useTextQuery,
      useSpatialIndex: Boolean= useSpatialIndex
  ): ImplementationSettings.DATASET = {
    
      if (database_location != "") {
      // if the directory does not exist, create it
      val currentRelativePath = Paths.get("");
      val abs = currentRelativePath.toAbsolutePath().toString();
      logger.debug("Current relative path is: " + abs);
      val dir = new File(abs, database_location)
      if (!dir.exists()) {
        logger.debug("creating database directory: " +
          database_location + " as " + dir + " - current (.) : " + new File(".").getAbsolutePath);
        dir.mkdirs()
      }

      logger.debug(s"""RDFStoreLocalJenaProvider: dataset create: database_location "$database_location" in ${System.getProperty("user.dir")} """)
      val dts =
        if (useTDB2)
          org.apache.jena.tdb2.TDB2Factory.connectDataset(database_location)
        else
          TDBFactory.createDataset(Paths.get(database_location).toString())
          // TODO TDBFactory.assembleDataset(assemblerFile)
      logger.debug(s"TDB Dataset created in '$database_location' in directory '$abs'")
      //Thread.dumpStack()
      logger.debug(s"RDFStoreLocalJenaProvider $database_location, dataset created: $dts")

      try {
        logger.debug(
          s"configureLuceneIndex, useTextQuery: $useTextQuery, useSpatialIndex: $useSpatialIndex")

        val res = configureLuceneIndex(dts, useTextQuery, useSpatialIndex)
          logger.debug(
          s"configureLuceneIndex DONE => $res")

        if (useSpatialIndex) {
          import org.apache.jena.geosparql.configuration._
          println(s"Before setupMemoryIndex")
          GeoSPARQLConfig.setupMemoryIndex // actually registers special SPARQL predicates!
          logger.info("isFunctionRegistered " + GeoSPARQLConfig.isFunctionRegistered)
          println(s"Before setupSpatialIndex")
          GeoSPARQLConfig.setupSpatialIndex(res)
          logger.info("findModeSRS <" + GeoSPARQLOperations.findModeSRS(res) + ">")
        }
        res
      } catch {
        case t: Throwable =>
          logger.error("!!!!! createDatabase: configureLuceneIndex: Exception: " +
              t.getLocalizedMessage)
          logger.error("	Exception class:" + t.getClass )
          logger.error("	Exception " + t )
          logger.error("	getCause " + t.getCause)
          logger.error("	>> Some indexing will not be available: Stack:")
          logger.error(s"${t.getStackTrace().slice(0, 12).mkString("\n")}")
          dts
      }
    } else
      DatasetFactory.createTxnMem()
  }

  lazy val dataset: DATASET = createDatabase(databaseLocation)

  /**
   * NOTES:
   *  - NEEDS transaction
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  - Union Graph in Jena should be re-done for each use (not 100% sure, but safer anyway)
   */
  override val allNamedGraph: Rdf#Graph = {
    time(s"allNamedGraph dataset $dataset", {
      //      logger.debug(s"Union Graph: entering for $dataset")

      // NOTE: very important to use the properly configured rdfStore (with defensiveCopy=false)
      val ang = rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph")).get
      //      logger.debug(s"Union Graph: hashCode ${ang.hashCode()} : size ${ang.size}")
      ang
    })
    //    union(dataset.getDefaultModel.getGraph :: unionGraph :: Nil)
  }

  /** List the names of graphs */
  def listNames(ds: DATASET): Iterator[String] = ds.listNames().asScala

  /** make an MGraph from a Dataset */
  def makeMGraph(graphURI: Rdf#URI, ds: DATASET = dataset): Rdf#MGraph = {
    logger.debug(s"makeMGraph( $graphURI")
    val nm = ds.getNamedModel(fromUri(graphURI))
    nm.getGraph
  }

  def close(ds: DATASET) = { println(s"close(ds=$ds");
//  ds.asDatasetGraph.close()
  ds.close() }

  def closeAllTDBs() {
    close(dataset)
    close(dataset2)
    close(dataset3)
    println(s"StopJenaTDB: dataset, dataset2, dataset3 closed.")
  }

  def syncTDB(ds: DATASET = dataset) = TDB.sync( ds )


  private val requestConfig =
    RequestConfig.custom()
      .setConnectTimeout(10 * 1000)
      .setConnectionRequestTimeout(10 * 1000)
//      .setConnectTimeout(defaultConnectTimeout)
      .setSocketTimeout(defaultReadTimeout)
      .build()

  /** TODO dataset is not used */
  def readWithContentType(
    uri: Rdf#URI,
    contentType: String,
    dataset: DATASET): Try[Rdf#Graph] =
    readWithContentTypeNoJena(uri, contentType)

  /** TODO dataset is not used */
  private def readWithContentTypeJena(
    uri: Rdf#URI,
    contentType: String,
    dataset: DATASET): Try[Rdf#Graph] = {
    Try {
      val sink = new TripleSink
      import org.apache.jena.riot.LangBuilder
      val contentTypeNoEncoding = contentType.replaceFirst(";.*", "")
      val lang = RDFLanguages.contentTypeToLang(contentTypeNoEncoding)
      logger.debug(s"readWithContentTypeJena: $lang , contentType $contentType, contentTypeNoEncoding $contentTypeNoEncoding")
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

  /** move it to specific trait
   *  TODO dataset is not used
   *   */
  private def readWithContentTypeNoJena(
    uri:         Rdf#URI,
    contentType: String): Try[Rdf#Graph] = {

    setTimeoutsFromConfig()

    val httpClient = CachingHttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy())
      .setDefaultRequestConfig(requestConfig)
      .build()
    val request = new HttpGet(fromUri(uri))

    // For Virtuoso :(
    val contentTypeNormalized = contentType.replaceAll(";.*", "")
    logger.debug(s"readWithContentTypeNoJena: uri <$uri> , contentTypeNormalized: $contentTypeNormalized")

    val tryResponse = Try {
      // TODO case of file:// URL
      request.addHeader("Accept", contentTypeNormalized)

      // For GogoCarto
      request.addHeader("X-Requested-With", "XMLHttpRequest")
      httpClient.execute(request)
    }

    tryResponse match {
      case Failure(f) => Failure(f)
      case Success(response) =>
        val reader0 = getReaderFromMIME(contentTypeNormalized)

        logger.debug(s"""readWithContentTypeNoJena:
                reader from HTTP header $reader0, ${reader0.getClass}
                request $request , getAllHeaders ${
                for (h <- request.getAllHeaders) logger.debug(h.toString()) }
                  response $response""")

        val reader/*From Extention*/ = if (!isKnownRdfSyntax(contentTypeNormalized)) {
          logger.error(
            s"readWithContentTypeNoJena($uri): no Reader found for contentType $contentType; trying from URI extension")
          val readerFromURI = getReaderFromURI(fromUri(withoutFragment(uri)))
          logger.debug( s"readWithContentTypeNoJena: Reader from URI extension: $readerFromURI")
          readerFromURI.getOrElse(reader0)
        } else reader0

        val inputStream = response.getEntity.getContent

        if (response.getStatusLine().getStatusCode != 200) {
          val rdr = new InputStreamReader(inputStream)
          val writer = new StringWriter()
          IOUtils.copy(inputStream, writer, "utf-8")
          logger.debug(s"readWithContentTypeNoJena: response $writer")
        }

        logger.debug(s"readWithContentTypeNoJena: reader $reader ${reader.getClass} =========")
        val res = reader.read(inputStream, fromUri(withoutFragment(uri)))
        logger.debug(s"readWithContentTypeNoJena: result $res =========")

        if (res.isFailure) {
          logger.debug(s"readWithContentTypeNoJena: reader.read() result: $res")
          val response = httpClient.execute(request)
          val inputStream = response.getEntity.getContent
          val rdr = new InputStreamReader(inputStream)
          val writer = new StringWriter()
          IOUtils.copy(inputStream, writer, "utf-8")
          val sampleFromContentReceived = writer.toString()
          logger.debug(s"readWithContentTypeNoJena: rdfString ${sampleFromContentReceived.substring(0,
              Math.min(2000, sampleFromContentReceived.length() )
              )}")
          reader.read(new StringReader(sampleFromContentReceived), fromUri(withoutFragment(uri)))
        } else
          res
    }
  }

}

