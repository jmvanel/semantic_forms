package deductions.runtime.jena
import org.apache.jena.tdb._

object RemoveGraphApp extends App {
  val dataset = TDBFactory.createDataset("TDB")
  dataset.removeNamedModel(args(0))
}
