package deductions.runtime.jena.lucene

import jena._
import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.query.text.DatasetGraphText
//import org.apache.jena.tdb.TDBFactory
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings

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

  val rdfStoreProvider = new ImplementationSettings.RDFCache {
    override val useTextQuery = true
  }

  val dataset0 = rdfStoreProvider.dataset
  val datasetWithLuceneConfigured = dataset0
  val graphWithLuceneConfigured = datasetWithLuceneConfigured.asDatasetGraph()
  println("datasetWithLuceneConfigured.asDatasetGraph() " + graphWithLuceneConfigured.getClass)
  val datasetGraphText: DatasetGraphText = graphWithLuceneConfigured.asInstanceOf[DatasetGraphText]
  dataset = datasetGraphText
  textIndex = dataset.getTextIndex()

  def doIndex() = {
    this.entityDefinition = rdfIndexing
    exec()
  }
}
