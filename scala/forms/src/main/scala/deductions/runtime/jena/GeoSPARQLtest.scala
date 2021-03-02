package deductions.runtime.jena

import org.apache.jena.geosparql.configuration.GeoSPARQLConfig
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.query.ReadWrite
import org.apache.jena.geosparql.configuration.GeoSPARQLOperations
import org.apache.jena.query.Dataset

/** cf doc https://jena.apache.org/documentation/geosparql/ */
object GeoSPARQLtest extends App {
  def geoQueryStr = """
    PREFIX spatial: <http://jena.apache.org/spatial#>
    SELECT * WHERE {
      ?feature spatial:withinBox( 43.0 0.0 48.0 10.0 100 )
    } LIMIT 100
  """

  lazy val datasetGeo = TDBFactory.createDataset( "TDBgeo" )
  testSuiteGeoSPARQL( datasetGeo)

  /** test Suite for GeoSPARQL; calls setupSpatialIndex() */
  def testSuiteGeoSPARQL(dataset: Dataset) = {
    load2Cities(dataset)
    GeoSPARQLConfig.setupSpatialIndex(dataset)
    GeoSPARQLConfig.setupMemoryIndex // actually registers special SPARQL predicates!
    println("isFunctionRegistered " + GeoSPARQLConfig.isFunctionRegistered)
    println("findModeSRS " + GeoSPARQLOperations.findModeSRS(dataset))

    // Alas, additions to TDB after setupSpatialIndex() are not indexed :( !!!
    println("Load RDF after setupSpatialIndex")
    dataset.begin(ReadWrite.WRITE)
    loadDBP("Massieux", dataset)
    dataset.commit()

    queryWithinBox()

    println("re-index (setupSpatialIndex)")
    GeoSPARQLConfig.setupSpatialIndex(dataset)

    queryWithinBox()
  }

  def load2Cities(dataset: Dataset) = {
    dataset.begin(ReadWrite.WRITE)
    loadDBP("Reyrieux", dataset)
    loadDBP("Parcieux", dataset)
    dataset.commit()
  }

  def loadDBP(id: String, dataset: Dataset=datasetGeo) = {
    val model = RDFDataMgr.loadModel("http://dbpedia.org/resource/" + id )
    dataset.getDefaultModel.add(model)
  }

  def queryWithinBox() = queryAndPrint(geoQueryStr)

  def queryAndPrint(queryStr: String, dataset: Dataset=datasetGeo) = {
    dataset.begin(ReadWrite.READ)
    val qe = QueryExecutionFactory.create(queryStr, dataset)
    val rs = qe.execSelect();
    ResultSetFormatter.outputAsTSV(rs)
    dataset.end()
  }
}