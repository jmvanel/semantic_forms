package deductions.runtime.jena

import org.w3.banana.jena.JenaStore
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.jena.JenaModule

object RDFStoreObject  extends JenaModule {
    lazy val dataset = TDBFactory.createDataset("TDB")
    lazy val store = JenaStore(dataset, defensiveCopy = true)
}