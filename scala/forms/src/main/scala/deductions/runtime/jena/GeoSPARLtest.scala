package deductions.runtime.jena

import org.apache.jena.geosparql.configuration.GeoSPARQLConfig
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.query.ReadWrite

/** cf doc https://jena.apache.org/documentation/geosparql/ */
object GeoSPARLtest extends App {
  val queryStr = """
    PREFIX spatial: <http://jena.apache.org/spatial#>
    SELECT * WHERE {
      ?feature spatial:withinBox( 43.0 0.0 48.0 10.0 100 )
    } LIMIT 100
  """

  val dataset = TDBFactory.createDataset( "TDBgeo" )
  def loadDBP(id: String) = {
    val model = RDFDataMgr.loadModel("http://dbpedia.org/resource/" + id )
    dataset.getDefaultModel.add(model)
  }
  dataset.begin(ReadWrite.WRITE)
    loadDBP("Reyrieux")
    loadDBP("Parcieux")
  dataset.commit()
  GeoSPARQLConfig.setupSpatialIndex(dataset)

  dataset.begin(ReadWrite.READ)
    val qe = QueryExecutionFactory.create(queryStr, dataset)
    val rs = qe.execSelect();
    ResultSetFormatter.outputAsTSV(rs)
  dataset.end()
}