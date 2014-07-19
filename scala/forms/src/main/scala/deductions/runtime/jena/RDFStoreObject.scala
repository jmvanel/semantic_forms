package deductions.runtime.jena

import org.w3.banana.jena.JenaStore
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.jena.JenaModule
import org.apache.log4j.Logger
import scala.collection.JavaConversions._

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends JenaModule {
  lazy val dataset = TDBFactory.createDataset("TDB")
  lazy val store = JenaStore(dataset, defensiveCopy = false)

  def printGraphList {
    Logger.getRootLogger().info(s"listGraphNodes store.dg.getClass() ${store.dg.getClass()}" )
    store.readTransaction {
      val lgn = store.dg.listGraphNodes()
      Logger.getRootLogger().info(s"listGraphNodes size ${lgn.size}")
      for (gn <- lgn) {
        Logger.getRootLogger().info(s"${gn.toString()}")
      }
      Logger.getRootLogger().info(s"afer listGraphNodes")
    }
  }
}