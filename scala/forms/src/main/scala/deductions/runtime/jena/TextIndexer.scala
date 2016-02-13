package deductions.runtime.jena
import jena._
import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.query.text.DatasetGraphText
import com.hp.hpl.jena.tdb.TDBFactory
import deductions.runtime.services.DefaultConfiguration
import org.w3.banana.jena.JenaModule

object TextIndexer extends jena.textindexer(Array[String]())
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

  def main(args: Array[String]) {
    doIndex()
  }

  def doIndex() = {
    this.entityDefinition = rdfIndexing
    exec()
  }
}
