package deductions.runtime.jena.lucene

import jena._
import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.query.text.DatasetGraphText
import org.apache.jena.tdb.TDBFactory
import deductions.runtime.services.DefaultConfiguration
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider

/**
 * Index TDB with Lucene a posteriori;
 *  see https://jena.apache.org/documentation/query/text-query.html
 */
object TextIndexerRDF extends App {
  val te = new TextIndexerTrait
  te.doIndex()
}

class TextIndexerTrait extends jena.textindexer(Array[String]())
    with JenaModule
    with LuceneIndex
    with DefaultConfiguration {

  val rdfStoreProvider = new RDFStoreLocalJena1Provider {
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
