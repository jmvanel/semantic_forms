package deductions.runtime.jena.lucene

import org.apache.jena.query.text.DatasetGraphText

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

/**
 * Index TDB with Lucene a posteriori;
 *  see https://jena.apache.org/documentation/query/text-query.html
 */
object TextIndexerRDF extends App {
  val te = new TextIndexerClass
  te.doIndex()
}

private[lucene] class TextIndexerClass extends jena.textindexer(Array[String]())
    with ImplementationSettings.RDFModule
    with LuceneIndex
    with DefaultConfiguration {

  val config = new DefaultConfiguration {
    override val useTextQuery = true
  }
  import config._

  val config1 = config
  // NOTE: this triggers configureLuceneIndex()
  val rdfStoreProvider = new ImplementationSettings.RDFCache {
    val config = config1
  }
  val dataset0 = rdfStoreProvider.dataset
  val datasetWithLuceneConfigured = dataset0

  val graphWithLuceneConfigured = datasetWithLuceneConfigured.asDatasetGraph()
    println("datasetWithLuceneConfigured.asDatasetGraph() getClass " + graphWithLuceneConfigured.getClass)
  val datasetGraphText: DatasetGraphText = graphWithLuceneConfigured.asInstanceOf[DatasetGraphText]

  // overrride jena.textindexer fields
  dataset = datasetGraphText
  textIndex = dataset.getTextIndex()

  def doIndex() = {
    this.entityDefinition = rdfIndexing
    exec()
  }
}
