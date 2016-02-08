package deductions.runtime.jena
import jena._
import org.apache.jena.query.text.EntityDefinition
import org.apache.jena.query.text.DatasetGraphText

object TextIndexer extends jena.textindexer(Array[String]()) // with App
{
  def main(args: Array[String]) {
    // TODO
  }

  def doIndex( entityDefinition: EntityDefinition, dataset: DatasetGraphText ) = {
    this.entityDefinition = entityDefinition
    this.dataset = dataset
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