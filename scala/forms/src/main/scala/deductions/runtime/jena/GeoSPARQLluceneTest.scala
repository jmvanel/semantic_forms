package deductions.runtime.jena

import deductions.runtime.jena.lucene.LuceneIndex
import org.apache.lucene.store.NIOFSDirectory
import java.nio.file.Paths

import org.apache.jena.query.text.TextIndexConfig
import org.apache.jena.query.text.TextDatasetFactory
import org.apache.jena.query.text.TextIndex
import deductions.runtime.utils.DefaultConfiguration
import org.apache.jena.query.Dataset

object GeoSPARQLluceneTest extends App
  with ImplementationSettings.RDFModule
  with LuceneIndex
{
  import GeoSPARQLtest._
  val config: deductions.runtime.utils.Configuration = new DefaultConfiguration{}

  val (textualDataset, textIndex) = {
      println(
          s"""configureLuceneIndex Textual: rdfIndexing getPredicates("text").size ${rdfIndexing.getPredicates("text").size}""")
      val directory = new NIOFSDirectory(Paths.get("LUCENEgeo"))
      val textIndexConfig = new TextIndexConfig(rdfIndexing)
      textIndexConfig.setMultilingualSupport(true)
      logger.info( ">>>> before createLuceneIndex")
      val textIndex: TextIndex = TextDatasetFactory.createLuceneIndex(
        directory, textIndexConfig)
      logger.info( s">>>> After createLuceneIndex $textIndex")
      (TextDatasetFactory.create(datasetGeo, textIndex,
            /* closeIndexOnDSGClose */ true), textIndex)
      }

  testSuiteGeoSPARQL( textualDataset )
  // load2Cities(textualDataset)
  testSuiteText( textualDataset )

  /** cf https://jena.apache.org/documentation/query/text-query.html */
  def testSuiteText(dataset: Dataset) = {
    println(s"Running Jena text:query")
    val textQueryStr = """
    PREFIX text: <http://jena.apache.org/text#>
    SELECT * WHERE {
      ?feature text:query ( 'Ain' ) .
    } LIMIT 100
  """
    queryAndPrint(textQueryStr, dataset)
  }
}