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
    with DefaultConfiguration //  with RDFStoreLocalJena1Provider // with App
    {
  val rdfStoreProvider = new RDFStoreLocalJena1Provider {
    override val useTextQuery = true
  }

  //  val databaseLocation: String = "TDB"
  //  val dataset0 = TDBFactory.createDataset(databaseLocation)
  val dataset0 = rdfStoreProvider.dataset
  val d = configureLuceneIndex(dataset0)
  val g = d.asDatasetGraph()
  println("d.asDatasetGraph() " + g.getClass)

  val datasetGraphText: DatasetGraphText =
    g.asInstanceOf[DatasetGraphText]
  dataset = datasetGraphText

  def main(args: Array[String]) {
    doIndex()
  }

  def doIndex() = {
    this.entityDefinition = rdfIndexing
    exec()
  }

  //  override val args = super[App].args  
  //  override val args: Array[String] = super[textindexer].args
  /*
object TextIndexer inherits conflicting members: variable args in class CmdLineArgs of type 
 java.util.Map[String,arq.cmdline.Arg] and method args in trait App of type ⇒ Array[String] (Note: this 
 can be resolved by declaring an override in object TextIndexer.)
    */
}