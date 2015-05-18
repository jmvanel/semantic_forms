package deductions.runtime.jena

import com.hp.hpl.jena.graph.{ Node => JenaNode, Triple => JenaTriple, _ }
import com.hp.hpl.jena.rdf.model.{ RDFReader => _ }
import java.io._
import org.apache.jena.riot._
import org.apache.jena.riot.system._
import org.w3.banana.io._
import org.w3.banana.jena.{ Jena, JenaOps }
import scala.util._

/** >>>> temporarily added this, pending Banana RDf pull request */
trait JenaRDFLoader {

  //  def makeRDFLoader() = new RDFLoader[Jena, Try] {
  /**
   * Read triples from the given location. The syntax is determined from input source URI
   *  (content negotiation or extension).
   */
  def load(url: java.net.URL): Try[Jena#Graph] = {
    Try(RDFDataMgr.loadGraph(url.toString))
  }
  //  }

}